from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import ClassSchedule, User
from app.auth_dependencies import require_permission # Updated dependency
from pydantic import BaseModel
from typing import List, Optional, Any
from datetime import date

router = APIRouter(prefix="/admin/timetable", tags=["Admin - Timetable"])

# --- Response Wrapper ---
def success_response(data: Any = None, message: str = "Success"):
    return {
        "success": True,
        "data": data,
        "message": message
    }

def error_response(code: str, message: str):
    return {
        "success": False,
        "error": {
            "code": code,
            "message": message
        }
    }

class ScheduleCreate(BaseModel):
    department: str
    year: int
    semester: Optional[Any] = None # Relaxed
    section: str
    day_of_week: str
    time_slot: str
    subject: str
    teacher_id: Optional[Any] = None # Relaxed
    period: Optional[str] = "1" 
    room: Optional[str] = None 
    date: Optional[str] = None # Relaxed to str

class ScheduleUpdate(BaseModel):
    day_of_week: Optional[str] = None
    time_slot: Optional[str] = None
    subject: Optional[str] = None
    teacher_id: Optional[Any] = None # Relaxed to Any
    period: Optional[str] = None
    room: Optional[str] = None
    semester: Optional[Any] = None # Relaxed to Any
    date: Optional[str] = None
    status: Optional[str] = None # PENDING, APPROVED, REJECTED

@router.post("/add")
def add_schedule_slot(
    req: ScheduleCreate,
    db: Session = Depends(get_db),
    user=Depends(require_permission("timetable:create"))
):
    # Basic validation
    existing = db.query(ClassSchedule).filter(
        ClassSchedule.department == req.department,
        ClassSchedule.year == req.year,
        ClassSchedule.section == req.section,
        ClassSchedule.day_of_week == req.day_of_week,
        ClassSchedule.time_slot == req.time_slot,
        ClassSchedule.date == req.date
    ).first()
    
    if existing:
        return error_response("CONFLICT", "Slot is already occupied in this class")

    new_schedule = ClassSchedule(
        department=req.department.strip(),
        year=req.year,
        semester=int(req.semester) if (req.semester and str(req.semester).isdigit()) else None,
        section=req.section.strip(),
        day_of_week=req.day_of_week.strip(),
        time_slot=req.time_slot.strip(),
        subject=req.subject.strip(),
        teacher_id=int(req.teacher_id) if (req.teacher_id and str(req.teacher_id).isdigit() and int(req.teacher_id) > 0) else None,
        period=req.period.strip() if req.period else "1",
        room=req.room.strip() if req.room else None,
        date=None
    )
    if req.date:
        if req.date.strip().lower() != "null" and req.date.strip() != "":
            from datetime import datetime
            try:
                new_schedule.date = datetime.strptime(req.date.strip(), "%Y-%m-%d").date()
            except ValueError:
                pass
    db.add(new_schedule)
    db.commit()
    return success_response({"id": new_schedule.id}, "Schedule slot added successfully")

@router.delete("/delete/{id}")
def delete_schedule_slot(
    id: int,
    db: Session = Depends(get_db),
    user=Depends(require_permission("timetable:update")) # Update/Delete usually grouped or separate
):
    slot = db.query(ClassSchedule).filter(ClassSchedule.id == id).first()
    if not slot:
        return error_response("NOT_FOUND", "Slot not found")
    
    db.delete(slot)
    db.commit()
    return success_response(message="Schedule slot deleted")

@router.put("/update/{id}")
def update_schedule_slot(
    id: int,
    req: ScheduleUpdate,
    db: Session = Depends(get_db),
    user=Depends(require_permission("timetable:update"))
):
    slot = db.query(ClassSchedule).filter(ClassSchedule.id == id).first()
    if not slot:
        return error_response("NOT_FOUND", "Slot not found")
    
    if req.day_of_week is not None: slot.day_of_week = req.day_of_week.strip()
    if req.time_slot is not None: slot.time_slot = req.time_slot.strip()
    if req.subject is not None: slot.subject = req.subject.strip()
    if req.teacher_id is not None:
        try:
            tid = int(req.teacher_id)
            slot.teacher_id = tid if tid > 0 else None
        except (ValueError, TypeError):
            slot.teacher_id = None
    if req.period is not None: slot.period = req.period.strip()
    if req.room is not None: slot.room = req.room.strip()
    if req.semester is not None:
        try:
            slot.semester = int(req.semester)
        except (ValueError, TypeError):
            pass
    if req.date is not None:
        if req.date.strip().lower() == "null" or req.date.strip() == "":
            slot.date = None
        else:
            from datetime import datetime
            try:
                slot.date = datetime.strptime(req.date.strip(), "%Y-%m-%d").date()
            except ValueError:
                pass
    if req.status is not None:
        # Check if status is changing to APPROVED
        if (slot.status != "APPROVED") and (req.status == "APPROVED") and slot.teacher_id:
            from app.models import Notification
            new_notif = Notification(
                user_id=slot.teacher_id,
                title="Class Request Approved",
                message=f"Your make-up class request for {slot.subject} on {slot.date} has been APPROVED.",
                type="SYSTEM"
            )
            db.add(new_notif)
        
        slot.status = req.status
    
    db.commit()
    return success_response(message="Schedule slot updated successfully")

@router.get("/list")
def get_schedule(
    department: str,
    year: int,
    section: str,
    semester: Optional[int] = Query(None),
    db: Session = Depends(get_db),
    user=Depends(require_permission("timetable:view_all"))
):
    from sqlalchemy import func
    query = db.query(ClassSchedule).filter(
        func.lower(ClassSchedule.department) == func.lower(department.strip()),
        ClassSchedule.year == year,
        func.lower(ClassSchedule.section) == func.lower(section.strip())
    )
    if semester:
        query = query.filter(ClassSchedule.semester == semester)
    slots = query.all()
    
    result = []
    for s in slots:
        s_name = s.subject # Default to acronym
        t_name = "Unknown"
        effective_teacher_id = s.teacher_id
        
        from app.models import Subject
        from sqlalchemy import or_
        subj_name_clean = s.subject.strip()
        subj = db.query(Subject).filter(
            or_(
                func.lower(Subject.name) == func.lower(subj_name_clean),
                func.lower(Subject.id) == func.lower(subj_name_clean)
            )
        ).first()

        if subj:
            s_name = subj.name
            if not effective_teacher_id:
                effective_teacher_id = subj.teacher_id

        if effective_teacher_id:
            t = db.query(User).filter(User.id == effective_teacher_id).first()
            if t: t_name = t.full_name or t.username
            
        result.append({
            "id": s.id,
            "department": s.department,
            "year": s.year,
            "semester": s.semester,
            "section": s.section,
            "day": s.day_of_week,
            "time": s.time_slot,
            "subject": s.subject,
            "subjectName": s_name,
            "teacher": t_name,
            "teacherId": effective_teacher_id,
            "period": s.period,
            "room": s.room,
            "date": str(s.date) if s.date else None,
            "status": s.status,
            "requestReason": s.request_reason
        })
        
    return success_response(result)
