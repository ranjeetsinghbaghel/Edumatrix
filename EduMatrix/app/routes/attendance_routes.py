from fastapi import APIRouter, BackgroundTasks
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.models import Student, Attendance, ClassSchedule
import re
import face_recognition
import numpy as np
import cv2
import base64
import time
from datetime import datetime, date, timedelta
from pydantic import BaseModel
from typing import Optional, List
from app.routes.admin_timetable import success_response, error_response
from app.utils.email_service import send_attendance_email

from app.face_recognition.recognize import KNOWN_ENCODINGS, KNOWN_STUDENT_IDS
last_marked = {} # RAM Cache for cooldown
COOLDOWN = 60 # seconds

router = APIRouter(prefix="/attendance", tags=["Attendance"])

class AttendanceRequest(BaseModel):
    image_base64: str
    subject_id: str
    period: str
    start_time: Optional[str] = None
    end_time: Optional[str] = None

# Helper to check window
def check_attendance_window(db: Session, subject_id: str, period: str, custom_start: str = None, custom_end: str = None):
    today = date.today()
    day_name = today.strftime("%A") # Monday, Tuesday...
    
    start_str = None
    end_str = None

    if custom_start and custom_end:
        start_str = custom_start
        end_str = custom_end
    else:
        # Check for specific date override (Make-up class)
        schedule = db.query(ClassSchedule).filter(
            ClassSchedule.date == today,
            ClassSchedule.subject == subject_id,
            ClassSchedule.period == period,
            ClassSchedule.status == "APPROVED"
        ).first()
        
        if not schedule:
            # Fallback to weekly schedule
            schedule = db.query(ClassSchedule).filter(
                ClassSchedule.day_of_week == day_name,
                ClassSchedule.subject == subject_id,
                ClassSchedule.period == period,
                ClassSchedule.date == None 
            ).first()

        if not schedule:
            return "OK", None
            
        try:
            start_str, end_str = schedule.time_slot.split("-")
        except:
            return "OK", None 

    try:
        start_time = datetime.strptime(start_str.strip(), "%H:%M").time()
        end_time = datetime.strptime(end_str.strip(), "%H:%M").time()
    except:
        return "OK", None
        
    now = datetime.now().time()
    
    start_dt = datetime.combine(today, start_time)
    end_dt = datetime.combine(today, end_time)
    now_dt = datetime.combine(today, now)
    
    grace = timedelta(minutes=15)
    
    if now_dt < start_dt - timedelta(minutes=10):
        return "EARLY", f"Class starts at {start_str}"
        
    if now_dt > end_dt:
        return "CLOSED", "Class has ended"
        
    if now_dt > start_dt + grace:
        return "LATE", "Grace period exceeded"
        
    return "OK", None

@router.post("/mark")
def mark_attendance_api(req: AttendanceRequest, background_tasks: BackgroundTasks):
    # Decode image
    try:
        header, encoded = req.image_base64.split(",", 1) if "," in req.image_base64 else (None, req.image_base64)
        img_bytes = base64.b64decode(encoded)
        img = cv2.imdecode(np.frombuffer(img_bytes, np.uint8), cv2.IMREAD_COLOR)
    except Exception as e:
        return error_response("DECODE_ERROR", f"Image decode error: {str(e)}")

    if img is None:
        return success_response({"success": False, "message": "Invalid image data"}, "Invalid image data")

    # Face Dictionary Logic
    try:
        height, width = img.shape[:2]
        max_width = 1600 # Increased from 800 to support group photos better
        if width > max_width:
            scale = max_width / width
            new_height = int(height * scale)
            img = cv2.resize(img, (max_width, new_height))
            
        rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        
        # Helper to try detection
        def detect_and_encode(image_rgb, tag="Original"):
            locs = face_recognition.face_locations(image_rgb, model="hog")
            if locs:
                print(f"   ✅ Found {len(locs)} faces in {tag} orientation.")
                return face_recognition.face_encodings(image_rgb, locs)
            return []

        # 1. Try Original
        encodings = detect_and_encode(rgb, "Original")

        # 2. If failed, try 90 degrees Clockwise (Common for phone landscape photos)
        if not encodings:
            print("   🔄 No faces. Retrying with 90° rotation...")
            rgb_90 = cv2.rotate(rgb, cv2.ROTATE_90_CLOCKWISE)
            encodings = detect_and_encode(rgb_90, "90° Clockwise")

        # 3. If failed, try 270 degrees (Counter-Clockwise)
        if not encodings:
            print("   🔄 No faces. Retrying with 270° rotation...")
            rgb_270 = cv2.rotate(rgb, cv2.ROTATE_90_COUNTERCLOCKWISE)
            encodings = detect_and_encode(rgb_270, "270° Counter-Clockwise")
            
        # 4. If failed, try 180 degrees
        if not encodings:
            print("   🔄 No faces. Retrying with 180° rotation...")
            rgb_180 = cv2.rotate(rgb, cv2.ROTATE_180)
            encodings = detect_and_encode(rgb_180, "180°")

        print(f"📸 Final Detection: Found {len(encodings)} faces.")

    except Exception as e:
         return success_response({"success": False, "message": f"Face processing error: {str(e)}"}, f"Error: {str(e)}")

    if not encodings:
        return success_response({"success": False, "message": "No face detected. Please ensure good lighting and face visibility."}, "No face detected in image")

    db = SessionLocal()
    now = time.time()
    today = date.today()

    try:
        # Validate Window
        status, reason = check_attendance_window(db, req.subject_id, req.period, req.start_time, req.end_time)
        if status == "EARLY" or status == "CLOSED":
            # FOR TESTING: Allow attendance even if closed, just mark as Late
            # return success_response({
            #     "success": False,
            #     "message": f"Cannot mark: {reason}"
            # }, f"Cannot mark: {reason}")
            is_late = True
            late_note = f"Diff time: {reason}"
        else:
            is_late = (status == "LATE")
            late_note = reason if is_late else None

        identified_students = []
        for encoding in encodings:
            if not KNOWN_ENCODINGS: 
                print("❌ No known faces loaded!")
                break # Prevent crash if no faces loaded
            
            distances = face_recognition.face_distance(KNOWN_ENCODINGS, encoding)
            if len(distances) == 0: continue
            
            idx = np.argmin(distances)
            min_dist = distances[idx]
            closest_id = KNOWN_STUDENT_IDS[idx]
            
            print(f"🔍 Face found. Best ID: {closest_id}, Dist: {min_dist:.4f}")

            if min_dist < 0.6: # Relaxed from 0.55 to 0.6 (Standard)
                student_id = closest_id
                if student_id not in identified_students:
                    identified_students.append(student_id)
            else:
                 print(f"   ⚠️ No match (Threshold 0.6)")

        if not identified_students:
             return error_response("NO_MATCH", "Faces detected but none recognized")

        success_count = 0
        newly_marked_names = []
        already_marked_names = []
        
        # Collect email data
        email_data_list = []

        for student_id in identified_students:
            s_obj = db.query(Student).filter(Student.id == student_id).first()
            
            # 1. Real Name for Emails
            real_name = s_obj.name if s_obj else f"ID({student_id})"
            
            # 2. Display Name (Roll No Suffix) for App Response
            s_name = real_name # Default fallback
            if s_obj and s_obj.roll_no:
                # Extract trailing digits: 23CSE706 -> 706
                match = re.search(r"(\d+)$", s_obj.roll_no)
                if match:
                    s_name = match.group(1)
                else:
                    s_name = s_obj.roll_no
            
             # Check cooldown
            cache_key = f"{student_id}_{req.subject_id}_{req.period}"
            if now - last_marked.get(cache_key, 0) < COOLDOWN:
                already_marked_names.append(s_name)
                continue

            # DB Check
            existing = db.query(Attendance).filter(
                Attendance.student_id == student_id,
                Attendance.subject == req.subject_id,
                Attendance.date == today,
                Attendance.period == req.period
            ).first()

            if existing:
                already_marked_names.append(s_name)
                continue
            
            # Prepare Email Data (Fetch User Email)
            user_email = s_obj.user.email if s_obj.user and s_obj.user.email else None
            guardian_email = s_obj.guardian_email
            

            # Mark
            attendance = Attendance(
                student_id=student_id,
                subject=req.subject_id,
                date=today,
                time=datetime.now().time(),
                period=req.period,
                is_late=is_late,
                late_reason=late_note,
                marked_at=datetime.utcnow()
            )
            db.add(attendance)
            success_count += 1
            last_marked[cache_key] = now
            newly_marked_names.append(s_name)
            
            if user_email or guardian_email:
                email_data_list.append({
                    "email": user_email,
                    "guardian_email": guardian_email,
                    "name": real_name, # Use real name for emails
                    "subject": req.subject_id,
                    "date": today.strftime("%Y-%m-%d"),
                    "time": datetime.now().strftime("%H:%M:%S"),
                    "status": "Late" if is_late else "Present"
                })
            
        db.commit()
        
        # Send Emails via Background Task
        for email_item in email_data_list:
            background_tasks.add_task(
                send_attendance_email,
                student_email=email_item["email"],
                student_name=email_item["name"],
                subject_name=email_item["subject"],
                date_str=email_item["date"],
                time_str=email_item["time"],
                status=email_item["status"],
                guardian_email=email_item["guardian_email"]
            )

        msg_parts = []
        if newly_marked_names:
            msg_parts.append(f"Marked: {', '.join(newly_marked_names)}")
        if already_marked_names:
            msg_parts.append(f"Already: {', '.join(already_marked_names)}")
            
        if not msg_parts:
            msg = "Faces detected but no valid student records found."
        else:
            msg = " | ".join(msg_parts)

        if is_late: msg += " (LATE)"

        return success_response({
            "success": True,
            "message": msg,
            "student_ids": identified_students,
            "count": success_count,
            "status": "Late" if is_late else "Present"
        }, msg)

    except Exception as e:
        db.rollback()
        return success_response({"success": False, "message": f"Server Error: {str(e)}"}, f"Server Error: {str(e)}")
    finally:
        db.close()

class ManualAttendanceRequest(BaseModel):
    roll_no: str
    subject_id: str
    period: str
    reason: Optional[str] = None 
    start_time: Optional[str] = None
    end_time: Optional[str] = None 

@router.post("/mark-manual")
def mark_attendance_manual(req: ManualAttendanceRequest, background_tasks: BackgroundTasks):
    db = SessionLocal()
    today = date.today()

    try:
        # Validate Window
        status, win_reason = check_attendance_window(db, req.subject_id, req.period, req.start_time, req.end_time)
        if status == "EARLY" or status == "CLOSED":
            # Return success=True (HTTP 200) so app parses it, but inner success=False
            return success_response({
                "success": False,
                "message": f"Cannot mark: {win_reason}" 
            }, f"Cannot mark: {win_reason}")
            
        is_late = (status == "LATE")
        late_note = req.reason if req.reason else win_reason
        
        student = db.query(Student).filter(Student.roll_no == req.roll_no).first()
        if not student:
            return error_response("NOT_FOUND", "Student not found with this roll no")

        existing = db.query(Attendance).filter(
            Attendance.student_id == student.id,
            Attendance.subject == req.subject_id,
            Attendance.date == today,
            Attendance.period == req.period
        ).first()

        if existing:
             return success_response(message=f"Attendance already exists for {student.name}")
        
        # Prepare Email Data
        user_email = student.user.email if student.user and student.user.email else None
        guardian_email = student.guardian_email

        attendance = Attendance(
            student_id=student.id,
            subject=req.subject_id,
            date=today,
            time=datetime.now().time(),
            period=req.period,
            is_late=is_late,
            late_reason=late_note,
            marked_at=datetime.utcnow()
        )
        db.add(attendance)
        db.commit()
        
        # Send Email
        if user_email or guardian_email:
            background_tasks.add_task(
                send_attendance_email,
                student_email=user_email,
                student_name=student.name,
                subject_name=req.subject_id,
                date_str=today.strftime("%Y-%m-%d"),
                time_str=datetime.now().strftime("%H:%M:%S"),
                status="Late" if is_late else "Present",
                guardian_email=guardian_email
            )

        msg = f"Recorded: {student.name}"
        if is_late: msg += " (Late)"

        return success_response({
            "success": True, 
            "message": msg,
            "student_id": student.id,
            "student_name": student.name,
            "status": "Late" if is_late else "Present"
        }, msg)

    except Exception as e:
        db.rollback()
        return error_response("INTERNAL_ERROR", str(e))
    finally:
        db.close()

@router.get("/subject/{subject_id}")
def get_subject_attendance(subject_id: str):
    db = SessionLocal()
    from app.models import Subject
    try:
        subject = db.query(Subject).filter(Subject.id == subject_id).first()
        if not subject:
            return error_response("SUBJECT_NOT_FOUND", "Subject not found")

        # All assigned students
        all_students = db.query(Student).filter(
            Student.department == subject.department,
            Student.year == subject.year,
            Student.section == subject.section
        ).all()
        student_map = {s.id: s for s in all_students}

        records = db.query(Attendance).filter(
            Attendance.subject == subject_id
        ).order_by(Attendance.date.desc(), Attendance.period).all()
        
        # Group by (date, period)
        sessions = {}
        for r in records:
            if not r.date: continue
            key = f"{r.date.isoformat()}_{r.period}"
            if key not in sessions:
                sessions[key] = {
                    "date": r.date.isoformat(),
                    "period": r.period,
                    "present": []
                }
            sessions[key]["present"].append(r.student_id)
            
        result_data = []
        for key, sess in sessions.items():
            present_ids = set(sess["present"])
            present_list = []
            absent_list = []
            for s_id, s_obj in student_map.items():
                s_dict = {"name": s_obj.name, "roll_no": s_obj.roll_no}
                if s_id in present_ids:
                    present_list.append(s_dict)
                else:
                    absent_list.append(s_dict)
            
            present_list.sort(key=lambda x: x["name"])
            absent_list.sort(key=lambda x: x["name"])
            
            result_data.append({
                "date": sess["date"],
                "period": sess["period"],
                "present": present_list,
                "absent": absent_list,
                "total_present": len(present_list),
                "total_absent": len(absent_list)
            })
            
        result_data.sort(key=lambda x: (x["date"], x["period"]), reverse=True)
            
        return success_response(result_data)
    finally:
        db.close()
