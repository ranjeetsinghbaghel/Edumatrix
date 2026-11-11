from datetime import datetime, date
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.models import Attendance, Student
from app.attendance.config import get_current_period_and_subject

def mark_attendance(student_id: int):
    db: Session = SessionLocal()

    try:
        print(" mark_attendance called for:", student_id)

        student = db.query(Student).filter(Student.id == student_id).first()
        if not student:
            print(" Student not found in DB:", student_id)
            return " Student not found "
        
         #  GET PERIOD & SUBJECT FROM CONFIG
        period, subject = get_current_period_and_subject()

        if not period:
            print(" No active class period right now")
            return "NO active period"


        today = date.today()

        # Prevent duplicate attendance for same day
        existing = db.query(Attendance).filter(
            Attendance.student_id == student_id,
            Attendance.date == today
        ).first()

        if existing:
            print(" Attendance already marked for this period ")
            return "Already marked"

        attendance = Attendance(
            student_id=student_id,
            subject=subject,           # TEMP (later dynamic)
            date=today,
            time=datetime.now().time(),
            period=period
        )

        db.add(attendance)
        db.commit()        
        db.refresh(attendance)

        print(" Attendance inserted successfully :", student_id, subject, period)
        return "Success"
    
    except Exception as e:
        db.rollback()
        print(" Attendance DB error:", e)
        return "DB Error"
    

        db.close()

import face_recognition
import numpy as np
import os

def save_face_encoding(identifier: str, image_path: str):
    image = face_recognition.load_image_file(image_path)
    encodings = face_recognition.face_encodings(image)
    
    if not encodings:
        raise Exception("No face detected in the image")
        
    encoding = encodings[0]
    
    # Robust Path
    CURRENT_FILE = os.path.abspath(__file__)
    APP_DIR = os.path.dirname(os.path.dirname(CURRENT_FILE))
    PROJECT_ROOT = os.path.dirname(APP_DIR)
    folder = os.path.join(PROJECT_ROOT, "images", "encodings")
    
    os.makedirs(folder, exist_ok=True)
    
    # Save as {id}.npy
    save_path = os.path.join(folder, f"{identifier}.npy")
    np.save(save_path, encoding)
    print(f"Saved encoding for {identifier} at {save_path}")

    # Update in-memory encodings incrementally
    val_id = identifier
    if str(identifier).isdigit():
        val_id = int(identifier)
        
    from app.face_recognition.recognize import add_or_update_face
    add_or_update_face(val_id, encoding)


