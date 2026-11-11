from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from datetime import date
from typing import Optional, List

from app.database import get_db
from app.models import Attendance, Student, Subject, User
from app.auth import admin_only

router = APIRouter(prefix="/admin/attendance", tags=["Admin - Attendance"])

@router.get("/all")
def view_all_attendance(
    attendance_date: Optional[date] = Query(None),
    subject_id: Optional[str] = Query(None),
    student_id: Optional[int] = Query(None),
    department: Optional[str] = Query(None),
    year: Optional[int] = Query(None),
    section: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    query = db.query(Attendance, Student).join(
        Student, Attendance.student_id == Student.id
    )

    if attendance_date:
        query = query.filter(Attendance.date == attendance_date)

    if subject_id:
        query = query.filter(Attendance.subject == subject_id)

    if student_id:
        query = query.filter(Student.id == student_id)

    if department:
        query = query.filter(Student.department == department)

    if year:
        query = query.filter(Student.year == year)

    if section:
        query = query.filter(Student.section == section)

    records = query.all()

    result = []
    for attendance, student in records:
        result.append({
            "attendance_id": attendance.id,
            "student_id": student.id,
            "student_name": student.name,
            "roll_no": student.roll_no,
            "department": student.department,
            "year": student.year,
            "section": student.section,
            "subject": attendance.subject,
            "date": attendance.date,
            "time": attendance.time,
            "period": attendance.period,
            "status": "Present"
        })

    return {
        "count": len(result),
        "records": result
    }

@router.get("/subject-summary")
def subject_wise_attendance(
    subject_id: str,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    total_present = db.query(Attendance).filter(
        Attendance.subject == subject_id
    ).count()

    students = db.query(Student).count()

    total_absent = students - total_present if students > total_present else 0

    return {
        "subject": subject_id,
        "total_students": students,
        "present": total_present,
        "absent": total_absent
    }

@router.get("/student-summary/{student_id}")
def student_attendance_summary(
    student_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        return {"error": "Student not found"}

    subjects = db.query(Attendance.subject).filter(
        Attendance.student_id == student_id
    ).distinct().all()

    data = []
    for (subject,) in subjects:
        attended = db.query(Attendance).filter(
            Attendance.student_id == student_id,
            Attendance.subject == subject
        ).count()

        data.append({
            "subject": subject,
            "attended_classes": attended
        })

    return {
        "student": student.name,
        "roll_no": student.roll_no,
        "attendance": data
    }

@router.get("/today")
def today_attendance(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    today = date.today()

    records = db.query(Attendance, Student).join(
        Student, Attendance.student_id == Student.id
    ).filter(Attendance.date == today).all()

    return [
        {
            "student_id": s.id,
            "name": s.name,
            "subject": a.subject,
            "period": a.period,
            "time": a.time
        }
        for a, s in records
    ]


