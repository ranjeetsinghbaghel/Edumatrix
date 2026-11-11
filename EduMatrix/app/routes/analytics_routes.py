from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func, case
from app.database import get_db
from app import models, schemas
from app.auth_dependencies import get_current_active_user, require_permission
from typing import List, Optional

router = APIRouter(prefix="/analytics", tags=["Analytics"])

@router.get("/attendance/summary", response_model=List[schemas.AttendanceSummary])
def get_attendance_summary(
    subject_id: Optional[str] = None,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = None
    
    # RBAC Logic for viewing scope
    if user.role == "student":
        # Student sees only self
        student = db.query(models.Student).filter(models.Student.user_id == user.id).first()
        if not student: return []
        student_id = student.id
    elif user.role == "teacher":
        # Teacher sees specific students (not implemented fully in this snippet, returning empty to avoid leak)
        # Ideally, teacher would pass student_id query param
        return [] 
    elif user.role == "admin":
        pass # Admin can see everything, usually filters by student_id in params

    # Query Construction
    # We want: Subject, Total Classes, Present, Absent, %
    # Group by Subject
    
    # Handle optional student_id param if Admin/Teacher
    # (Implementation simplified for Student View)
    
    if student_id:
        query = db.query(
            models.Attendance.subject,
            func.count(models.Attendance.id).label("total"),
            func.sum(case((models.Attendance.status == "Present", 1), else_=0)).label("present")
        ).filter(models.Attendance.student_id == student_id)
        
        if subject_id:
            query = query.filter(models.Attendance.subject == subject_id)
            
        results = query.group_by(models.Attendance.subject).all()
        
        summary = []
        for row in results:
            total = row.total or 0
            present = row.present or 0
            absent = total - present
            percent = (present / total * 100) if total > 0 else 0.0
            
            summary.append(schemas.AttendanceSummary(
                total_classes=total,
                present=present,
                absent=absent,
                percentage=round(percent, 2),
                subject_id=row.subject
            ))
        return summary
        
    return []

@router.get("/defaulters", response_model=List[schemas.DefaulterEntry])
def get_defaulters(
    subject_id: str,
    threshold: float = 75.0,
    user: models.User = Depends(require_permission("DEFALTER_VIEW_SUBJECT")),
    db: Session = Depends(get_db)
):
    # 1. Verification: Is teacher allowed for this subject? (Skipped for brevity)
    
    # 2. Get students in the class (Department/Year/Section lookup via Subject)
    # Assuming Subject has dep/year/sec fields
    subject = db.query(models.Subject).filter(models.Subject.id == subject_id).first()
    if not subject: raise HTTPException(404, "Subject not found")
    
    students = db.query(models.Student).filter(
        models.Student.department == subject.department,
        models.Student.year == subject.year,
        models.Student.section == subject.section
    ).all()
    
    defaulters = []
    for s in students:
        # Count stats
        total = db.query(models.Attendance).filter(
            models.Attendance.student_id == s.id,
            models.Attendance.subject == subject_id
        ).count()
        
        present = db.query(models.Attendance).filter(
            models.Attendance.student_id == s.id,
            models.Attendance.subject == subject_id,
            models.Attendance.status == "Present"
        ).count()
        
        if total == 0: continue
        
        pct = (present / total) * 100
        if pct < threshold:
            defaulters.append(schemas.DefaulterEntry(
                student_id=s.id,
                name=s.name,
                roll_no=s.roll_no,
                percentage=round(pct, 2),
                total_classes=total,
                absent=total - present,
                guardian_name=s.guardian_name,
                guardian_mobile=s.guardian_mobile,
                guardian_email=s.guardian_email
            ))
            
    return defaulters
