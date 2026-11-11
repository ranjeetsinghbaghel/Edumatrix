from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app import models, schemas
from app.auth_dependencies import get_current_active_user
from datetime import date, timedelta
from typing import List

router = APIRouter(prefix="/insights", tags=["Smart Insights"])

@router.get("/trend", response_model=List[schemas.TrendPoint])
def get_attendance_trend(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    if user.role != "student":
        raise HTTPException(status_code=400, detail="Only students have attendance trends")
    
    student = db.query(models.Student).filter(models.Student.user_id == user.id).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student profile not found")

    dept = student.department.strip() if student.department else ""
    sec = student.section.strip() if student.section else ""
    
    valid_subjects = db.query(models.Subject).filter(
        func.lower(models.Subject.department) == func.lower(dept),
        models.Subject.year == student.year,
        func.lower(models.Subject.section) == func.lower(sec)
    ).all()
    valid_subject_ids = [s.id for s in valid_subjects]

    conducted_classes = db.query(
        models.Attendance.subject, models.Attendance.date, models.Attendance.period
    ).join(models.Student, models.Attendance.student_id == models.Student.id).filter(
        func.lower(models.Student.department) == func.lower(dept),
        models.Student.year == student.year,
        func.lower(models.Student.section) == func.lower(sec),
        models.Attendance.subject.in_(valid_subject_ids)
    ).distinct().all()

    my_presence = db.query(
        models.Attendance.subject, models.Attendance.date, models.Attendance.period
    ).filter(
        models.Attendance.student_id == student.id,
        models.Attendance.subject.in_(valid_subject_ids),
        func.lower(models.Attendance.status).in_(["present", "late"])
    ).all()

    from itertools import groupby
    from datetime import date
    
    conducted_classes.sort(key=lambda x: x.date or date.min)
    present_set = {(p.subject, p.date, p.period) for p in my_presence}
    
    trend_points = []
    total_so_far = 0
    present_so_far = 0
    
    for date_key, group in groupby(conducted_classes, key=lambda x: x.date):
        if not date_key: continue
        day_conducted = list(group)
        total_so_far += len(day_conducted)
        present_today = sum(1 for c in day_conducted if (c.subject, c.date, c.period) in present_set)
        present_so_far += present_today
        
        percentage = (present_so_far / total_so_far) * 100 if total_so_far > 0 else 0
        trend_points.append(schemas.TrendPoint(date=date_key, percentage=round(percentage, 2)))
        
    return trend_points

@router.get("/risk", response_model=schemas.StudentRisk)
def get_risk_analysis(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    if user.role != "student":
         return schemas.StudentRisk(risk_level="LOW", message="Not a student", missable_classes=0, current_percentage=0.0)

    student = db.query(models.Student).filter(models.Student.user_id == user.id).first()
    
    dept = student.department.strip() if student.department else ""
    sec = student.section.strip() if student.section else ""
    
    valid_subjects = db.query(models.Subject).filter(
        func.lower(models.Subject.department) == func.lower(dept),
        models.Subject.year == student.year,
        func.lower(models.Subject.section) == func.lower(sec)
    ).all()
    valid_subject_ids = [s.id for s in valid_subjects]

    total = db.query(
        models.Attendance.subject, models.Attendance.date, models.Attendance.period
    ).join(models.Student, models.Attendance.student_id == models.Student.id).filter(
        func.lower(models.Student.department) == func.lower(dept),
        models.Student.year == student.year,
        func.lower(models.Student.section) == func.lower(sec),
        models.Attendance.subject.in_(valid_subject_ids)
    ).distinct().count()
    
    present = db.query(
        models.Attendance.subject, models.Attendance.date, models.Attendance.period
    ).filter(
        models.Attendance.student_id == student.id, 
        models.Attendance.subject.in_(valid_subject_ids),
        func.lower(models.Attendance.status).in_(["present", "late"])
    ).distinct().count()
    
    if total == 0:
        return schemas.StudentRisk(risk_level="LOW", message="No classes yet", missable_classes=10, current_percentage=0.0)
        
    current_pct = (present / total) * 100
    TARGET = 75.0
    
    # Calculate how many classes can be missed before dropping below 75%
    # (P) / (T + Missed) >= 0.75
    # P >= 0.75 * (T + Missed)
    # P/0.75 >= T + Missed
    # Missed <= (P/0.75) - T
    
    missable = int((present / 0.75) - total)
    if missable < 0: missable = 0
    
    risk_level = "LOW"
    message = f"You are safe! You can miss {missable} classes."
    
    if current_pct < TARGET:
        risk_level = "HIGH"
        diff = TARGET - current_pct
        message = f"Critical! You are {diff:.1f}% short of target."
        missable = 0
    elif missable < 3:
        risk_level = "MEDIUM"
        message = f"Caution! Only {missable} classes safe to miss."
        
    return schemas.StudentRisk(
        risk_level=risk_level,
        message=message,
        missable_classes=missable,
        current_percentage=round(current_pct, 2)
    )

@router.get("/required", response_model=schemas.RequiredClasses)
def get_required_classes(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    if user.role != "student":
         return schemas.RequiredClasses(target_percentage=75.0, required_classes=0)

    student = db.query(models.Student).filter(models.Student.user_id == user.id).first()
    
    dept = student.department.strip() if student.department else ""
    sec = student.section.strip() if student.section else ""
    
    valid_subjects = db.query(models.Subject).filter(
        func.lower(models.Subject.department) == func.lower(dept),
        models.Subject.year == student.year,
        func.lower(models.Subject.section) == func.lower(sec)
    ).all()
    valid_subject_ids = [s.id for s in valid_subjects]

    total = db.query(
        models.Attendance.subject, models.Attendance.date, models.Attendance.period
    ).join(models.Student, models.Attendance.student_id == models.Student.id).filter(
        func.lower(models.Student.department) == func.lower(dept),
        models.Student.year == student.year,
        func.lower(models.Student.section) == func.lower(sec),
        models.Attendance.subject.in_(valid_subject_ids)
    ).distinct().count()
    
    present = db.query(
        models.Attendance.subject, models.Attendance.date, models.Attendance.period
    ).filter(
        models.Attendance.student_id == student.id, 
        models.Attendance.subject.in_(valid_subject_ids),
        func.lower(models.Attendance.status).in_(["present", "late"])
    ).distinct().count()
    
    TARGET = 75.0 # In percentage
    TARGET_RATIO = 0.75
    
    if total == 0:
         return schemas.RequiredClasses(target_percentage=TARGET, required_classes=0)
         
    current_pct = (present / total) * 100
    
    if current_pct >= TARGET:
        return schemas.RequiredClasses(target_percentage=TARGET, required_classes=0)
        
    # (P + R) / (T + R) >= 0.75
    # P + R >= 0.75T + 0.75R
    # 0.25R >= 0.75T - P
    # R >= (0.75T - P) / 0.25
    
    required = (TARGET_RATIO * total - present) / (1 - TARGET_RATIO)
    import math
    required = math.ceil(required)
    
    if required < 0: required = 0
    
    return schemas.RequiredClasses(target_percentage=TARGET, required_classes=int(required))
