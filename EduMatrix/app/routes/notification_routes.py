from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from app.database import get_db
from app import models, schemas, firebase_utils
from app.auth_dependencies import get_current_active_user, require_permission
from typing import List
from datetime import datetime
from sqlalchemy import case, desc

router = APIRouter(prefix="/notifications", tags=["Notifications"])

@router.get("/", response_model=List[schemas.NotificationOut])
def get_notifications(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    return db.query(models.Notification)\
        .filter(models.Notification.user_id == user.id)\
        .order_by(models.Notification.created_at.desc())\
        .limit(50).all()

@router.get("/notices", response_model=List[schemas.NoticeOut])
def get_notices(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    now = datetime.utcnow()
    
    # Base query for Expiry & Scheduling
    # Should not show future scheduled notices
    query = db.query(models.Notice).filter(
        (models.Notice.expiry_date == None) | (models.Notice.expiry_date > now),
        (models.Notice.scheduled_at == None) | (models.Notice.scheduled_at <= now),
        models.Notice.is_template == False
    )
    
    # Role & Targeting Logic
    if user.role == "student":
        student = db.query(models.Student).filter(models.Student.user_id == user.id).first()
        if not student:
            # Fallback if student record missing but user is student role (shouldn't happen)
            query = query.filter((models.Notice.target_audience == "ALL") | (models.Notice.target_audience == "STUDENTS"))
        else:
            # Complex filtering:
            # 1. Audience matches (ALL or STUDENTS)
            # 2. Dept matches (ALL or specific)
            # 3. Year matches (ALL or specific)
            # 4. Section matches (ALL or specific)
            query = query.filter(
                (models.Notice.target_audience.in_(["ALL", "STUDENTS"])),
                (models.Notice.department == None) | (models.Notice.department == student.department),
                (models.Notice.year == None) | (models.Notice.year == student.year),
                (models.Notice.section == None) | (models.Notice.section == student.section)
            )
            
    elif user.role == "teacher":
         # Teachers see ALL, TEACHERS, and potentiallydept specific if we add that later
         # For now, just role based + loose dept matching if implemented
        query = query.filter((models.Notice.target_audience == "ALL") | (models.Notice.target_audience == "TEACHERS"))
    elif user.role == "admin":
        pass # Admin sees all
        
    # Priority Sorting: HIGH > MEDIUM > LOW > Date
    priority_order = case(
        (models.Notice.priority == 'HIGH', 1),
        (models.Notice.priority == 'MEDIUM', 2),
        (models.Notice.priority == 'LOW', 3),
        else_=4
    )
    
    notices = query.order_by(priority_order, models.Notice.scheduled_at.desc(), models.Notice.created_at.desc()).limit(50).all()
    
    # Check read status efficiently
    read_ids = {r[0] for r in db.query(models.NoticeRead.notice_id).filter(models.NoticeRead.user_id == user.id).all()}
    
    result = []
    for n in notices:
        n_out = schemas.NoticeOut.from_orm(n)
        n_out.is_read = n.id in read_ids
        n_out.view_count = len(n.reads)
        result.append(n_out)
        
    return result

@router.post("/notices/{notice_id}/read")
def mark_notice_read(
    notice_id: int,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    # Check if notice exists
    notice = db.query(models.Notice).filter(models.Notice.id == notice_id).first()
    if not notice:
        raise HTTPException(status_code=404, detail="Notice not found")

    # Check if already read
    existing = db.query(models.NoticeRead).filter(
        models.NoticeRead.notice_id == notice_id,
        models.NoticeRead.user_id == user.id
    ).first()
    
    if not existing:
        try:
            new_read = models.NoticeRead(notice_id=notice_id, user_id=user.id)
            db.add(new_read)
            db.commit()
        except Exception:
            db.rollback()
            # Ignore race conditions
            pass
        
    return {"status": "marked read"}

@router.get("/notices/{notice_id}/readers", response_model=List[schemas.NoticeReaderOut])
def get_notice_readers(
    notice_id: int,
    user: models.User = Depends(require_permission("USER_UPDATE")), # Admin only
    db: Session = Depends(get_db)
):
    readers = db.query(models.NoticeRead).filter(models.NoticeRead.notice_id == notice_id).order_by(models.NoticeRead.read_at.desc()).all()
    
    result = []
    for r in readers:
        name = "Unknown"
        role = "Unknown"
        if r.user:
            name = r.user.full_name if r.user.full_name else r.user.username
            role = r.user.role
            
        result.append(schemas.NoticeReaderOut(
            user_id=r.user_id,
            name=name,
            role=role,
            read_at=r.read_at
        ))
        
    return result


@router.post("/send-notice")
def send_notice(
    notice_data: schemas.NoticeCreate,
    background_tasks: BackgroundTasks,
    admin: models.User = Depends(require_permission("USER_UPDATE")), # Admin only
    db: Session = Depends(get_db)
):
    # 1. Save Notice
    new_notice = models.Notice(
        title=notice_data.title,
        message=notice_data.message,
        sender_id=admin.id,
        target_audience=notice_data.target_audience,
        image_url=notice_data.image_url,
        priority=notice_data.priority,
        expiry_date=notice_data.expiry_date,
        department=notice_data.department,
        year=notice_data.year,
        section=notice_data.section,
        scheduled_at=notice_data.scheduled_at,
        is_template=notice_data.is_template,
        template_name=notice_data.template_name
    )
    db.add(new_notice)
    db.commit()
    db.refresh(new_notice)
    
    # 2. Check Schedule or Template
    if notice_data.is_template:
        return {"message": "Template saved successfully", "id": new_notice.id}
        
    if notice_data.scheduled_at and notice_data.scheduled_at > datetime.utcnow():
        return {"message": "Notice scheduled successfully", "scheduled_at": notice_data.scheduled_at}

    # 3. Get Tokens with Targeting
    # We want tokens for valid users based on audience AND filters
    
    # Start with User
    base_query = db.query(models.User.fcm_token).filter(
        models.User.fcm_token.isnot(None), 
        models.User.fcm_token != ""
    )
    
    if notice_data.target_audience == "STUDENTS":
        # Join with Student table to filter by dept/year/section
        stmt = base_query.join(models.Student, models.Student.user_id == models.User.id).filter(models.User.role == "student")
        
        if notice_data.department:
            stmt = stmt.filter(models.Student.department == notice_data.department)
        if notice_data.year:
            stmt = stmt.filter(models.Student.year == notice_data.year)
        if notice_data.section:
            stmt = stmt.filter(models.Student.section == notice_data.section)
            
        tokens = [t[0] for t in stmt.all()]
        
    elif notice_data.target_audience == "TEACHERS":
        stmt = base_query.filter(models.User.role == "teacher")
        # Could filter by Teacher details if we linked them, but for now simple role
        tokens = [t[0] for t in stmt.all()]
    else:
        # ALL
         tokens = [t[0] for t in base_query.all()]
    
    # 4. Send FCM in background (Immediate)
    if tokens:
         # Exclude sender's own token if testing on single device
         if admin.fcm_token:
             tokens = [tok for tok in tokens if tok != admin.fcm_token]
             
    if tokens:
        is_emergency = notice_data.priority == "HIGH"
        notification_type = "EMERGENCY" if is_emergency else "NOTICE"
        
        background_tasks.add_task(
            firebase_utils.send_multicast_notification,
            tokens=tokens,
            title=f"[{notice_data.priority}] {notice_data.title}" if is_emergency else notice_data.title,
            body=notice_data.message,
            image_url=notice_data.image_url,
            data={
                "type": notification_type, 
                "notice_id": str(new_notice.id),
                "priority": notice_data.priority
            },
            is_emergency=is_emergency
        )
        
    return {"message": "Notice sent successfully", "recipient_count": len(tokens)}
