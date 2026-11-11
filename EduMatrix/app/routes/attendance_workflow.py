from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app import models, schemas
from app.auth_dependencies import get_current_active_user, require_permission
from app.audit_logger import log_action
from datetime import datetime
from typing import List

router = APIRouter(prefix="/workflow", tags=["Attendance Workflow"])

# 1. Start Class Session (Teacher)
@router.post("/session/start", response_model=schemas.ClassSessionOut)
def start_session(
    session_in: schemas.ClassSessionCreate,
    user: models.User = Depends(require_permission("ATTENDANCE_MARK")),
    db: Session = Depends(get_db)
):
    # Close any existing active sessions primarily
    existing = db.query(models.ClassSession).filter(
        models.ClassSession.teacher_id == user.id,
        models.ClassSession.status == "ACTIVE"
    ).first()
    
    if existing:
        existing.status = "CLOSED"
        existing.end_time = datetime.now()
    
    new_session = models.ClassSession(
        teacher_id=user.id,
        subject_id=session_in.subject_id,
        department=session_in.department,
        year=session_in.year,
        section=session_in.section,
        status="ACTIVE"
    )
    db.add(new_session)
    db.commit()
    db.refresh(new_session)
    return new_session

# 2. Mark Attendance (Face / Manual)
@router.post("/attendance/bulk-upload")
def upload_attendance(
    upload_data: schemas.SessionAttendanceUpload,
    background_tasks: BackgroundTasks,
    user: models.User = Depends(require_permission("ATTENDANCE_MARK")),
    db: Session = Depends(get_db)
):
    session = db.query(models.ClassSession).filter(models.ClassSession.id == upload_data.session_id).first()
    if not session or session.status != "ACTIVE":
        raise HTTPException(400, "Invalid or closed session")
        
    records = []
    for sid in upload_data.student_ids:
        # Check duplicate
        exists = db.query(models.Attendance).filter(
            models.Attendance.student_id == sid,
            models.Attendance.session_id == session.id
        ).first()
        
        if not exists:
            rec = models.Attendance(
                student_id=sid,
                session_id=session.id,
                subject=session.subject_id,
                date=session.date,
                status="Present"
            )
            records.append(rec)
            
            # Create notification task
            background_tasks.add_task(notify_student, sid, session.subject_id, session.date)
    
    if records:
        db.add_all(records)
        db.commit()
        log_action(db, user.id, "MARK_ATTENDANCE", "SESSION", str(session.id), {"count": len(records)})
        
    return {"message": f"Marked {len(records)} students"}

def notify_student(student_id_val: int, subject: str, date_val):
    # Create notification logic
    pass # Implementation in notification_routes

# 3. Request Correction
@router.post("/correction/request")
def request_correction(
    req: schemas.CorrectionRequestCreate,
    user: models.User = Depends(require_permission("CORRECTION_REQUEST")),
    db: Session = Depends(get_db)
):
    new_req = models.CorrectionRequest(
        requester_id=user.id,
        student_id=req.student_id,
        subject_id=req.subject_id,
        date=req.date,
        requested_status=req.requested_status,
        reason=req.reason,
        current_status="Unknown" # Logic to fetch current
    )
    db.add(new_req)
    db.commit()
    return {"message": "Correction requested"} 
