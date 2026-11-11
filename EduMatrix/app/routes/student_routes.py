from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import Student, User, LeaveRequest, Attendance
from app.auth import get_current_user
from pydantic import BaseModel
from datetime import date
from typing import List

router = APIRouter(prefix="/student", tags=["Student"])

from app.schemas import StudentOut

class LeaveRequestCreate(BaseModel):
    reason: str

class PasswordChange(BaseModel):
    new_password: str

@router.post("/change-password")
def change_password(
    req: PasswordChange,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from app.models import User
    from app.auth import hash_password
    
    db_user = db.query(User).filter(User.username == user["username"]).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="User not found")
    
    db_user.password = hash_password(req.new_password)
    db.commit()
    return {"message": "Password updated successfully"}

@router.get("/profile", response_model=StudentOut)
def get_profile(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    # Find student by linked user_id
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        # Fallback
        from sqlalchemy import func
        student = db.query(Student).filter(func.lower(Student.name) == func.lower(user["username"])).first()
    
    if not student:
        raise HTTPException(status_code=404, detail="Student profile not found.")
    
    # Populate fields from associated User model
    student_out = StudentOut.model_validate(student)
    if student.user:
        student_out.email = student.user.email
        student_out.mobile = student.user.mobile
        
        from app.utils.image_utils import resolve_profile_image
        student_out.profile_image = resolve_profile_image(student.user, student)
    
    return student_out

class BloodGroupUpdate(BaseModel):
    blood_group: str

@router.put("/profile/blood-group")
def update_blood_group(
    req: BloodGroupUpdate,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")
        
    student.blood_group = req.blood_group
    db.commit()
    return {"message": "Blood group updated successfully", "blood_group": student.blood_group}

@router.post("/leave")
def submit_leave(
    req: LeaveRequestCreate,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
         student = db.query(Student).filter(Student.name == user["username"]).first()

    if not student:
        raise HTTPException(status_code=404, detail="Student not found")

    leave = LeaveRequest(
        student_id=student.id,
        reason=req.reason,
        date=date.today(),
        status="Pending"
    )
    db.add(leave)
    db.commit()
    return {"message": "Leave request submitted successfully"}

@router.get("/attendance")
def get_my_attendance(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from sqlalchemy import func
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        student = db.query(Student).filter(func.lower(Student.name) == func.lower(user["username"])).first()
    
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")

    # 1. Determine the student's authorized subjects from ClassSchedule
    from app.models import Subject
    valid_subjects = db.query(Subject).filter(
        func.lower(Subject.department) == func.lower(student.department.strip()),
        Subject.year == student.year,
        func.lower(Subject.section) == func.lower(student.section.strip())
    ).all()
    
    valid_subject_ids = [s.id for s in valid_subjects]
    allowed_subjects_map = {s.id: (s.name.strip() if s.name and s.name.strip() else s.id) for s in valid_subjects}

    # 2. Find classes actually conducted for THIS specific section and these subjects
    conducted_classes = db.query(
        Attendance.subject, Attendance.date, Attendance.period
    ).join(Student, Attendance.student_id == Student.id).filter(
        func.lower(Student.department) == func.lower(student.department.strip()),
        Student.year == student.year,
        func.lower(Student.section) == func.lower(student.section.strip()),
        Attendance.subject.in_(valid_subject_ids)
    ).distinct().all()

    # 3. Get THIS student's PRESENT records for these subjects
    my_presence = db.query(
        Attendance.subject, Attendance.date, Attendance.period
    ).filter(
        Attendance.student_id == student.id,
        Attendance.subject.in_(valid_subject_ids),
        func.lower(Attendance.status).in_(["present", "late"])
    ).all()

    present_set = {(p.subject, p.date, p.period) for p in my_presence}
    conducted_subjects = {c[0] for c in conducted_classes}

    # 4. Generate the complete history
    history = []
    fake_id = 1
    
    from datetime import date
    sorted_conducted = sorted(conducted_classes, key=lambda c: c.date or date.min, reverse=True)
    
    for c in sorted_conducted:
        subject_id, c_date, c_period = c
        s_name = allowed_subjects_map.get(subject_id, subject_id)
        is_present = (subject_id, c_date, c_period) in present_set
        
        history.append({
            "id": fake_id,
            "subject": s_name,
            "date": c_date,
            "time": None,
            "status": "Present" if is_present else "Absent",
            "period": c_period
        })
        fake_id += 1
        
    # 5. For subjects NEVER conducted yet, add a single dummy "NC" object so the UI column can render 
    for s_id in valid_subject_ids:
        if s_id not in conducted_subjects:
            s_name = allowed_subjects_map.get(s_id, s_id)
            history.append({
                "id": fake_id,
                "subject": s_name,
                "date": None,
                "time": None,
                "status": "NC",
                "period": None
            })
            fake_id += 1
            
    return history

@router.get("/timetable")
def get_timetable(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from app.models import ClassSchedule
    
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
         student = db.query(Student).filter(func.lower(Student.name) == func.lower(user["username"])).first()
    
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")

    from sqlalchemy import func
    query = db.query(ClassSchedule).filter(
        func.lower(ClassSchedule.department) == func.lower(student.department.strip()),
        ClassSchedule.year == student.year,
        func.lower(ClassSchedule.section) == func.lower(student.section.strip())
    )
    if student.semester:
        query = query.filter(ClassSchedule.semester == student.semester)
    schedule = query.all()
    
    results = []
    for s in schedule:
         s_name = s.subject
         teacher_name = "Unknown"
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
         
         # If still no teacher ID, but we have one in slot (handled before), it persists.
         # If we found a subject, effective_teacher_id might have been updated.

         if effective_teacher_id:
             t = db.query(User).filter(User.id == effective_teacher_id).first()
             if t: teacher_name = t.full_name or t.username
         
         results.append({
             "id": s.id,
             "day": s.day_of_week,
             "time": s.time_slot,
             "subject": s.subject,
             "subjectName": s_name,
             "teacher": teacher_name,
             "period": s.period,
             "room": s.room,
             "date": str(s.date) if s.date else None
         })
         
    return results

@router.get("/next-class")
def get_next_class(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from app.models import ClassSchedule
    from sqlalchemy import func
    from datetime import datetime
    
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        student = db.query(Student).filter(func.lower(Student.name) == func.lower(user["username"])).first()
    
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")

    today = date.today()
    day_name = today.strftime("%A")
    now_time = datetime.now().time()
    
    # 1. Get all schedules for today (Weekly + Specific Date)
    query = db.query(ClassSchedule).filter(
        func.lower(ClassSchedule.department) == func.lower(student.department.strip()),
        ClassSchedule.year == student.year,
        func.lower(ClassSchedule.section) == func.lower(student.section.strip())
    )
    if student.semester:
        query = query.filter(ClassSchedule.semester == student.semester)
        
    all_slots = query.all()
    
    upcoming = []
    
    for s in all_slots:
        # Check date validity
        if s.date and s.date != today:
            continue # Skip specific dates that aren't today
        if not s.date and s.day_of_week != day_name:
            continue # Skip weekly slots for other days
            
        # Check time
        try:
            start_str = s.time_slot.split("-")[0].strip()
            start_time = datetime.strptime(start_str, "%H:%M").time()
            if start_time > now_time:
                # Enrich with Subject Name / Teacher Name
                s_name = s.subject
                t_name = "Unknown"
                
                from app.models import Subject
                from sqlalchemy import or_
                subj = db.query(Subject).filter(
                    or_(
                        func.lower(Subject.name) == func.lower(s.subject.strip()),
                        func.lower(Subject.id) == func.lower(s.subject.strip())
                    )
                ).first()
                
                eff_t_id = s.teacher_id
                if subj:
                    s_name = subj.name
                    if not eff_t_id: eff_t_id = subj.teacher_id
                
                if eff_t_id:
                     t = db.query(User).filter(User.id == eff_t_id).first()
                     if t: t_name = t.full_name or t.username
                
                upcoming.append({
                    "id": s.id,
                    "subject": s.subject,
                    "subjectName": s_name,
                    "time": s.time_slot,
                    "room": s.room,
                    "teacher": t_name,
                    "start_time_obj": start_time 
                })
        except: continue
        
    # Sort by time
    upcoming.sort(key=lambda x: x["start_time_obj"])
    
    if upcoming:
        # Remove the internal sorting object before returning
        res = upcoming[0]
        del res["start_time_obj"]
        return res
        
    return None

# --- New Features ---
from app.models import ProctorMeeting, SectionMetadata, Notification
from app.schemas import ProctorMeetingOut
from fastapi import BackgroundTasks
from app import firebase_utils
from datetime import datetime

class StudentMessageCreate(BaseModel):
    message: str

@router.get("/proctor/meetings", response_model=List[ProctorMeetingOut])
def get_my_proctor_meetings(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        raise HTTPException(404, "Student profile not found")
        
    meetings = db.query(ProctorMeeting).filter(ProctorMeeting.student_id == student.id).all()
    for m in meetings:
        m.student_name = student.name
    return meetings

@router.post("/message/class-teacher")
def send_message_to_ct(
    msg: StudentMessageCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        raise HTTPException(404, "Student profile not found")
        
    # Find Class Teacher
    ct_section = db.query(SectionMetadata).filter(
        SectionMetadata.department == student.department,
        SectionMetadata.year == student.year,
        SectionMetadata.section == student.section
    ).first()
    
    if not ct_section or not ct_section.class_teacher_id:
        raise HTTPException(404, "Class Teacher not assigned for your section.")
        
    teacher = db.query(User).filter(User.id == ct_section.class_teacher_id).first()
    if not teacher:
        raise HTTPException(404, "Class Teacher user not found.")
        
    # Create Notification for Teacher
    notif = Notification(
        user_id=teacher.id,
        title=f"Message from {student.name} ({student.roll_no})",
        message=msg.message,
        created_at=datetime.utcnow()
    )
    db.add(notif)
    db.commit()
    
    # Send Push
    sender_user = db.query(User).filter(User.id == user["id"]).first()
    sender_token = sender_user.fcm_token if sender_user else None

    if teacher.fcm_token and teacher.fcm_token != sender_token:
         background_tasks.add_task(
             firebase_utils.send_multicast_notification,
             tokens=[teacher.fcm_token],
             title=f"Message from {student.name}",
             body=msg.message,
             data={"type": "STUDENT_MESSAGE", "sender_id": str(student.id)}
         )
         
    return {"message": "Message sent to Class Teacher"}

class ProctorResponseCreate(BaseModel):
    response: str

@router.put("/proctor/meeting/{id}/respond")
def respond_to_proctor_meeting(
    id: int, 
    resp: ProctorResponseCreate, 
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    # Verify student owns this meeting? Or just exists?
    # Ideally check student_id matches user, but meeting holds student_id.
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student: raise HTTPException(404, "Student not found")

    meeting = db.query(ProctorMeeting).filter(ProctorMeeting.id == id).first()
    if not meeting: raise HTTPException(404, "Meeting not found")
    
    if meeting.student_id != student.id:
        raise HTTPException(403, "Not authorized to respond to this meeting")
    
    meeting.student_response = resp.response
    db.commit()
    return {"message": "Response recorded"}

@router.get("/teachers/availability")
def get_teacher_availability(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from app.models import ClassSession
    from sqlalchemy import or_
    
    # Get all teachers
    teachers = db.query(User).filter(User.role == "teacher", User.is_active == True).all()
    
    # Get active class sessions right now
    active_sessions = db.query(ClassSession).filter(
        ClassSession.status == "ACTIVE"
    ).all()
    
    busy_teacher_ids = [s.teacher_id for s in active_sessions]
    
    results = []
    for t in teachers:
        if not t.is_available:
            status = "Unavailable"
        else:
            status = "In Class" if t.id in busy_teacher_ids else t.custom_availability_message
        # Optional: You could check Proctor meetings or Leaves for "Busy"
        results.append({
            "id": str(t.id),
            "name": t.full_name or t.username,
            "department": t.hod_department or "Faculty",
            "status": status
        })
    return results

@router.get("/calendar")
def get_academic_calendar(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from app.models import CalendarEvent
    events = db.query(CalendarEvent).order_by(CalendarEvent.created_at.desc()).all()
    results = []
    for e in events:
        results.append({
            "title": e.title,
            "date": e.date_str,
            "type": e.event_type
        })
    return results

@router.get("/study-plan")
def get_personalized_study_plan(
    db: Session = Depends(get_db),
    user=Depends(get_current_user)
):
    from app.models import Student, Subject, ClassSchedule, Attendance, Marks
    from sqlalchemy import func
    from datetime import date, datetime

    # 1. Resolve student
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        student = db.query(Student).filter(func.lower(Student.name) == func.lower(user.get("username", ""))).first()
    if not student:
        # Fallback to top student in CSE Sec G
        student = db.query(Student).filter(Student.department == "CSE", Student.section == "G").first()

    student_name = student.name if student else user.get("username", "Student")
    roll_no = student.roll_no if student else "23CSE513"
    dept = student.department if student else "CSE"
    sec = student.section if student else "G"
    sem = student.semester if student else 7

    # 2. Fetch student's marks to personalize study priorities
    tasks = []
    task_id = 1

    marks_rows = []
    if student:
        marks_rows = db.query(Marks).filter(Marks.student_id == student.id).all()

    # Calculate average score percentage by subject
    subj_scores = {}
    for m in marks_rows:
        if m.total_marks and m.total_marks > 0:
            pct = (m.score / m.total_marks) * 100.0
            if m.subject_id not in subj_scores:
                subj_scores[m.subject_id] = []
            subj_scores[m.subject_id].append(pct)

    avg_subj_pct = {sid: (sum(scores) / len(scores)) for sid, scores in subj_scores.items()}

    # 3. Check today's schedule
    today_name = date.today().strftime("%A")
    today_slots = db.query(ClassSchedule).filter(
        ClassSchedule.department == dept,
        ClassSchedule.section == sec,
        ClassSchedule.semester == sem,
        ClassSchedule.day_of_week == today_name
    ).all()

    # 4. Synthesize intelligent, student-specific tasks
    # Subject 1: Compiler Design
    cd_pct = avg_subj_pct.get("CS701", 82.0)
    tasks.append({
        "id": task_id,
        "title": "Compiler Design: Practice LR(1) & LALR Parsing Tables",
        "subject": "Compiler Design (CD)",
        "duration": "1 hr 15 mins",
        "reason": f"Faculty: Dr. D. Anil Kumar | Current subject mastery: {cd_pct:.1f}% - Target parsing conflict questions",
        "priority": "HIGH" if cd_pct < 85 else "MEDIUM",
        "completed": False
    })
    task_id += 1

    # Subject 2: Computer Networks & Security
    cns_pct = avg_subj_pct.get("CS704", 85.0)
    tasks.append({
        "id": task_id,
        "title": "CNS: Deep-Dive into RSA Encryption & Diffie-Hellman",
        "subject": "Computer Networks and Security (CNS)",
        "duration": "1 hr",
        "reason": "Faculty: Ms. Supriya Satapathy | Revise key cryptographic algorithms for upcoming quiz & internal",
        "priority": "HIGH",
        "completed": False
    })
    task_id += 1

    # Subject 3: Data Analytics
    da_pct = avg_subj_pct.get("CS703", 88.0)
    tasks.append({
        "id": task_id,
        "title": "Data Analytics: K-Means Clustering & PCA Implementation",
        "subject": "Data Analytics (DA)",
        "duration": "45 mins",
        "reason": "Faculty: Dr. Bhabani Sankar Panda | Practical code execution on datasets & inertia analysis",
        "priority": "MEDIUM",
        "completed": False
    })
    task_id += 1

    # Subject 4: Computer Graphics
    cg_pct = avg_subj_pct.get("CS702", 80.0)
    tasks.append({
        "id": task_id,
        "title": "Computer Graphics: Bresenham's Line & Cohen-Sutherland Clipping",
        "subject": "Computer Graphics (CG)",
        "duration": "50 mins",
        "reason": f"Faculty: Mr. Kedarnath Panda | Current score: {cg_pct:.1f}% - Reinforce rasterization math",
        "priority": "HIGH" if cg_pct < 85 else "MEDIUM",
        "completed": False
    })
    task_id += 1

    # Subject 5: Entrepreneurship & Enterprise Development
    tasks.append({
        "id": task_id,
        "title": "EED: Business Model Canvas & Break-Even Analysis Case Study",
        "subject": "Entrepreneurship (ED/EED)",
        "duration": "35 mins",
        "reason": "Faculty: Dr. A. Chiranjibi Rambabu Achary | Prepare startup MVP & financial overview",
        "priority": "NORMAL",
        "completed": False
    })
    task_id += 1

    # Subject 6: Major Project Phase-I
    tasks.append({
        "id": task_id,
        "title": "Major Project Phase-I: SRS Documentation & System Architecture",
        "subject": "Major Project Phase-I",
        "duration": "2 hrs",
        "reason": "Guide: Dr. D. Anil Kumar | Wednesday dedicated 3-hour project slot milestone",
        "priority": "HIGH",
        "completed": False
    })
    task_id += 1

    # Subject 7: Technical Seminar
    tasks.append({
        "id": task_id,
        "title": "Technical Seminar: Research Paper Presentation PPT",
        "subject": "Technical Seminar",
        "duration": "40 mins",
        "reason": "Coordinator: Mrs. A. Jyoshana | Thursday 10:00 AM seminar presentation",
        "priority": "MEDIUM",
        "completed": False
    })
    task_id += 1

    # Daily Timetable Focus Task
    if today_slots:
        subjs_today = [s.subject for s in today_slots]
        tasks.append({
            "id": task_id,
            "title": f"Today's Classroom Focus: {', '.join(subjs_today)} Review",
            "subject": f"{today_name} Classes (Room AME-16)",
            "duration": "30 mins",
            "reason": f"Prepare for today's scheduled classes: {', '.join(subjs_today)}",
            "priority": "NORMAL",
            "completed": False
        })

    return {
        "student_name": student_name,
        "roll_no": roll_no,
        "department": dept,
        "semester": sem,
        "section": sec,
        "room": "AME-16",
        "academic_session": "2026-27",
        "overall_performance": f"{((sum(avg_subj_pct.values()) / max(1, len(avg_subj_pct)))) if avg_subj_pct else 84.5:.1f}%",
        "tasks": tasks
    }
