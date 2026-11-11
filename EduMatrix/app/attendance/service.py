from datetime import datetime, date
from sqlalchemy.orm import Session
from app.models import Attendance, ClassSchedule, Student

def mark_attendance_service(student_id: int, db: Session):

    today = date.today()
    now_time = datetime.now().time()

    # 1️⃣ Verify student exists
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        return None

    # 2️⃣ Find today’s class
    schedule = db.query(ClassSchedule).filter(
        ClassSchedule.date == today,
        ClassSchedule.department == student.department,
        ClassSchedule.year == student.year,
        ClassSchedule.section == student.section
    ).first()

    if not schedule:
        return None

    # 3️⃣ Prevent duplicate attendance
    already = db.query(Attendance).filter(
        Attendance.student_id == student_id,
        Attendance.date == today,
        Attendance.period == schedule.period
    ).first()

    if already:
        return already

    # 4️⃣ Create attendance
    record = Attendance(
        student_id=student_id,
        subject=schedule.subject,
        date=today,
        time=now_time,
        period=schedule.period
    )

    db.add(record)
    db.commit()
    db.refresh(record)

    return record
