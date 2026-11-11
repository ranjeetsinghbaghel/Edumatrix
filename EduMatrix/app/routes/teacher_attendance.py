from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from datetime import date, datetime

from app.database import get_db
from app.models import Attendance, Student, Subject, ClassSchedule
from app.auth import teacher_only

router = APIRouter(prefix="/teacher/attendance", tags=["Teacher - Attendance"])

@router.get("/subjects")
def teacher_subjects(
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    subjects = db.query(Subject).filter(
        Subject.teacher_id == user["id"]
    ).all()

    return [
        {
            "id": s.id,
            "name": s.name
        }
        for s in subjects
    ]


from app.schemas import AttendanceMark, BulkAttendanceMark, AttendanceStudentOut
from typing import List, Optional
from sqlalchemy import func
from sqlalchemy import func
from app.routes.admin_timetable import success_response
from app.utils.image_utils import resolve_profile_image

@router.get("/students", response_model=List[AttendanceStudentOut])
def get_students_by_class(
    year: int,
    section: str,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    from sqlalchemy.orm import joinedload
    students = db.query(Student).filter(
        Student.year == year,
        func.lower(Student.section) == func.lower(section)
    ).options(joinedload(Student.user)).all()

    from app.utils.image_utils import resolve_profile_image
    
    results = []
    for s in students:
        s_out = AttendanceStudentOut.model_validate(s)
        s_out.profile_image = resolve_profile_image(s.user, s)
        results.append(s_out)
        
    return results

@router.post("/mark-bulk")
def mark_bulk_attendance(
    data: BulkAttendanceMark,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    from app.utils.email_service import send_attendance_email  # Local import to avoid circular deps

    student_ids = data.student_ids
    subject_id = data.subject_id
    period = data.period
    today = date.today()
    
    # helper info
    subject_obj = db.query(Subject).filter(Subject.id == subject_id).first()
    subject_name = subject_obj.name if subject_obj else subject_id
    
    successful = 0
    for stud_id in student_ids:
        exists = db.query(Attendance).filter(
            Attendance.student_id == stud_id,
            Attendance.subject == subject_id,
            Attendance.date == today,
            Attendance.period == period
        ).first()
        
        if not exists:
            attendance = Attendance(
                student_id=stud_id,
                subject=subject_id,
                date=today,
                time=datetime.now().time(),
                period=period
            )
            db.add(attendance)
            successful += 1
            
            # Send Email
            student = db.query(Student).filter(Student.id == stud_id).first()
            if student:
                student_email = student.user.email if (student.user and student.user.email) else None
                
                # Add to background task
                background_tasks.add_task(
                    send_attendance_email,
                    student_email=student_email,
                    student_name=student.name,
                    subject_name=subject_name,
                    date_str=str(today),
                    time_str=datetime.now().strftime("%I:%M %p"),
                    status="Present",
                    guardian_email=student.guardian_email
                )
            
    db.commit()
    return {"message": f"Valid attendance marked for {successful} students", "success": "true"}


@router.post("/mark")
def mark_attendance(
    data: AttendanceMark,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    student_id = data.student_id
    subject_id = data.subject_id
    period = data.period
    today = date.today()

    already_marked = db.query(Attendance).filter(
        Attendance.student_id == student_id,
        Attendance.subject == subject_id,
        Attendance.date == today,
        Attendance.period == period
    ).first()

    if already_marked:
        return {"message": "Attendance already marked", "success": "false"}

    attendance = Attendance(
        student_id=student_id,
        subject=subject_id,
        date=today,
        time=datetime.now().time(),
        period=period
    )

    db.add(attendance)
    db.commit()

    return {"message": "Attendance marked successfully", "success": "true"}

@router.get("/today/{subject_id}")
def today_attendance(
    subject_id: str,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    today = date.today()

    records = db.query(Attendance, Student).join(
        Student, Attendance.student_id == Student.id
    ).filter(
        Attendance.subject == subject_id,
        Attendance.date == today
    ).all()

    return [
        {
            "student_id": s.id,
            "name": s.name,
            "roll_no": s.roll_no,
            "period": a.period,
            "time": a.time,
            "profile_image": resolve_profile_image(s.user, s)
        }
        for a, s in records
    ]

@router.get("/present-absent/{subject_id}")
def present_absent_students(
    subject_id: str,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    today = date.today()
    
    # 1. Get Subject to know the Class (Dept/Year/Sec)
    subject = db.query(Subject).filter(Subject.id == subject_id).first()
    if not subject:
         raise HTTPException(status_code=404, detail="Subject not found")

    # 2. Get TOTAL students in that Class
    query = db.query(Student)
    
    # Filter only if the subject has specific class details assigned
    if subject.department:
        query = query.filter(Student.department == subject.department)
    if subject.year:
        query = query.filter(Student.year == subject.year)
    if subject.section:
        query = query.filter(Student.section == subject.section)
        
    all_students_in_class = query.all()

    # 3. Get PRESENT students for this subject today
    present_records = db.query(Attendance).filter(
        Attendance.subject == subject_id,
        Attendance.date == today
    ).all()
    
    present_ids = {a.student_id for a in present_records}

    # 4. Separate Present vs Absent
    present_list = []
    absent_list = []
    
    from app.utils.image_utils import resolve_profile_image
    
    for s in all_students_in_class:
        info = {
            "id": s.id, 
            "name": s.name, 
            "roll_no": s.roll_no,
            "profile_image": resolve_profile_image(s.user, s)
        }
        if s.id in present_ids:
            present_list.append(info)
        else:
            absent_list.append(info)

    return {
        "present": present_list,
        "absent": absent_list
    }

@router.get("/history")
def attendance_history(
    subject_id: str,
    start_date: Optional[date] = None,
    end_date: Optional[date] = None,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    query = db.query(Attendance, Student).join(
        Student, Attendance.student_id == Student.id
    ).filter(Attendance.subject == subject_id)

    if start_date:
        query = query.filter(Attendance.date >= start_date)
    if end_date:
        query = query.filter(Attendance.date <= end_date)
        
    records = query.order_by(Attendance.date.desc(), Attendance.time.desc()).all()

    return success_response([
        {
            "id": a.id,
            "date": a.date,
            "time": a.time,
            "student_id": s.id,
            "student_name": s.name,
            "roll_no": s.roll_no,
            "period": a.period,
            "status": "Late" if a.is_late else "Present",
            "is_late": a.is_late,
            "late_reason": a.late_reason,
            "profile_image": resolve_profile_image(s.user, s)
        }
        for a, s in records
    ])
