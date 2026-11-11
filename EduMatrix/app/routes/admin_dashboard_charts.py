from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import Attendance, Student, ClassSchedule
from app.auth import admin_only
from pydantic import BaseModel

from app.models import LeaveRequest
from sqlalchemy import func
from app.models import Attendance


router = APIRouter(prefix="/admin/charts", tags=["Admin Charts"])

@router.get("/overview")
def attendance_overview(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    total_students = db.query(Student).count()
    total_classes = db.query(ClassSchedule).count()
    total_attendance = db.query(Attendance).count()

    overall_percentage = (
        (total_attendance / (total_students * total_classes)) * 100
        if total_students and total_classes else 0
    )

    return {
        "total_students": total_students,
        "total_classes": total_classes,
        "attendance_marked": total_attendance,
        "overall_percentage": round(overall_percentage, 2)
    }

@router.get("/subject-wise")
def subject_wise_attendance(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    data = db.query(
        Attendance.subject,
        func.count(Attendance.id)
    ).group_by(Attendance.subject).all()

    return {
        "subjects": [d[0] for d in data],
        "attendance_count": [d[1] for d in data]
    }

@router.get("/daily")
def daily_attendance(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    data = db.query(
        Attendance.date,
        func.count(Attendance.id)
    ).group_by(Attendance.date).order_by(Attendance.date).all()

    return {
        "dates": [str(d[0]) for d in data],
        "attendance": [d[1] for d in data]
    }

@router.get("/department-year")
def department_year_attendance(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    data = db.query(
        Student.department,
        Student.year,
        func.count(Attendance.id)
    ).join(
        Attendance, Attendance.student_id == Student.id
    ).group_by(
        Student.department, Student.year
    ).all()

    result = {}
    for dept, year, count in data:
        key = f"{dept} - Year {year}"
        result[key] = count

    return result

@router.get("/leaves")
def leave_statistics(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    approved = db.query(LeaveRequest).filter(
        LeaveRequest.status == "Approved"
    ).count()

    rejected = db.query(LeaveRequest).filter(
        LeaveRequest.status == "Rejected"
    ).count()

    pending = db.query(LeaveRequest).filter(
        LeaveRequest.status == "Pending"
    ).count()

    return {
        "approved": approved,
        "rejected": rejected,
        "pending": pending
    }

from sqlalchemy import case

@router.get("/defaulters")
def defaulter_count(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    from sqlalchemy import case, func
    from app.models import Student
    
    # Calculate attendance per student directly from their records
    stmt = db.query(
        Attendance.student_id,
        func.count(Attendance.id).label('total'),
        func.sum(case((Attendance.status == 'Present', 1), else_=0)).label('present')
    ).group_by(Attendance.student_id).all()
    
    defaulter_ids = []
    safe = 0
    
    # Calculate percentages handling integers properly
    for student_id, total, present in stmt:
        if total and total > 0:
            present_count = present or 0
            pct = float(present_count) / float(total)
            if pct < 0.75:
                defaulter_ids.append((student_id, pct * 100))
            else:
                safe += 1
                
    defaulters_list = []
    if defaulter_ids:
        s_ids = [did for did, _ in defaulter_ids]
        students_db = db.query(Student).filter(Student.id.in_(s_ids)).all()
        student_map = {s.id: s for s in students_db}
        
        for did, pct in defaulter_ids:
            if did in student_map:
                s = student_map[did]
                defaulters_list.append({
                    "id": s.id,
                    "name": s.name,
                    "department": s.department,
                    "year": s.year,
                    "section": s.section,
                    "rollNo": s.roll_no,
                    "percentage": round(pct, 2),
                    "guardian_name": s.guardian_name,
                    "guardian_mobile": s.guardian_mobile,
                    "guardian_email": s.guardian_email
                })

    return {
        "defaulters": len(defaulters_list), 
        "safe": safe,
        "students": defaulters_list
    }

class NotifyDefaultersRequest(BaseModel):
    student_ids: list[int]

@router.post("/defaulters/notify")
def notify_defaulters(
    req: NotifyDefaultersRequest,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    from app.models import Notice, User, Student
    from datetime import datetime
    
    students = db.query(Student).filter(Student.id.in_(req.student_ids)).all()
    if not students:
        return {"success": False, "message": "No valid students found"}
        
    admin_id = user.get("id", 1)
    notified_hods = set()
    
    for s in students:
        # 1. Find the HOD for this student's department
        if s.department:
            hod = db.query(User).filter(User.is_hod == 1, User.hod_department == s.department).first()
            if hod and hod.id not in notified_hods:
                # 2. Create a specific Notice for this HOD
                msg = f"URGENT: Defaulters identified in your department ({s.department}). Please check the admin logs and issue warnings."
                notice = Notice(
                    title="Low Attendance Alert",
                    message=msg,
                    sender_id=admin_id,
                    target_audience="HOD",
                    department=s.department,
                    priority="HIGH",
                    created_at=datetime.utcnow()
                )
                db.add(notice)
                notified_hods.add(hod.id)
                
    db.commit()
    return {"success": True, "message": f"Successfully notified {len(notified_hods)} HOD(s) for the selected students."}

@router.get("/most-absent")
def most_absent_subject(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    # Calculate average attendance % per subject
    stmt = db.query(
        Attendance.subject,
        func.count(Attendance.id).label('total'),
        func.sum(case((Attendance.status == 'Present', 1), else_=0)).label('present')
    ).group_by(Attendance.subject).all()
    
    result = []
    for subj, total, present in stmt:
        pct = (present/total)*100 if total > 0 else 0
        result.append({"subject": subj, "percentage": round(pct, 2)})
        
    # Sort ascending (lowest attendance first)
    result.sort(key=lambda x: x['percentage'])
    
    return result[:5] # Top 5 worst subjects


