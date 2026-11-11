from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session
from datetime import date

from app.database import get_db
from app.models import Attendance, Student, User, ClassSchedule
from app.auth import admin_or_teacher, get_current_user
from fastapi.templating import Jinja2Templates

from app.schemas import StudentCreate, StudentOut

router = APIRouter(prefix="/admin", tags=["Admin"])
templates = Jinja2Templates(directory="app/templates")


# ---------------- DASHBOARD DATA ----------------
@router.get("/dashboard-data")
def admin_dashboard_data(
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    from datetime import timedelta
    from app.models import Student 
    today = date.today()

    total_students = db.query(Student).count()
    total_teachers = db.query(User).filter(User.role == "teacher").count()
    
    # "Classes" here refers to total scheduled classes/sessions
    active_classes = db.query(ClassSchedule).count()

    # Attendance Trends (Last 5 Days)
    attendance_trends = []
    for i in range(4, -1, -1):
        day_date = today - timedelta(days=i)
        count = db.query(Attendance).filter(Attendance.date == day_date).count()
        attendance_trends.append({
            "day": day_date.strftime("%a"),
            "count": count
        })
        
    return {
        "roleAnalytics": {
            "totalStudents": total_students,
            "totalTeachers": total_teachers,
            "activeClasses": active_classes
        },
        "attendanceTrends": attendance_trends
    }


# ---------------- DASHBOARD PAGE ----------------
@router.get("/dashboard")
def admin_dashboard(request: Request, user=Depends(admin_or_teacher)):
    return templates.TemplateResponse(
        "admin_dashboard.html",
        {"request": request}
    )


# ---------------- GET ALL STUDENTS REMOVED ----------------
# Use /admin/students/ endpoint from admin_students.py


# ---------------- STUDENT ATTENDANCE ----------------
@router.get("/student-attendance/{student_id}")
def get_student_attendance(
    student_id: int,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    Depends(admin_or_teacher)


    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        return {"error": "Student not found"}

    subjects = db.query(Attendance.subject).filter(
        Attendance.student_id == student_id
    ).distinct().all()

    subject_data = []
    total_attended = 0
    total_classes = 0

    for (subject,) in subjects:
        attended = db.query(Attendance).filter(
            Attendance.student_id == student_id,
            Attendance.subject == subject
        ).count()

        total = db.query(ClassSchedule).filter(
            ClassSchedule.subject == subject
        ).count()

        percentage = (attended / total * 100) if total else 0

        subject_data.append({
            "subject": subject,
            "attended": attended,
            "total": total,
            "percentage": round(percentage, 2)
        })

        total_attended += attended
        total_classes += total

    overall_percentage = (
        (total_attended / total_classes) * 100
        if total_classes else 0
    )

    return {
        "student_id": student.id,
        "name": student.name,
        "subjects": subject_data,
        "overall_percentage": round(overall_percentage, 2)
    }
# ---------------- STUDENT MANAGEMENT REMOVED ----------------
# Logic is now handled in app/routes/admin_students.py to avoid duplication and conflicts.

# ---------------- CALENDAR MANAGEMENT ----------------
from app.schemas import CalendarEventCreate, CalendarEventOut
from app.models import CalendarEvent

@router.post("/calendar", response_model=CalendarEventOut)
def add_calendar_event(
    event: CalendarEventCreate,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    new_event = CalendarEvent(
        title=event.title,
        date_str=event.date_str,
        event_type=event.event_type
    )
    db.add(new_event)
    db.commit()
    db.refresh(new_event)
    return new_event

@router.delete("/calendar/{event_id}")
def delete_calendar_event(
    event_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    ev = db.query(CalendarEvent).filter(CalendarEvent.id == event_id).first()
    if not ev: 
        return {"error": "Event not found"}
    db.delete(ev)
    db.commit()
    return {"message": "Event deleted successfully"}
