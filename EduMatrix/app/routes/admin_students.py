import shutil
import os
import cv2
import numpy as np
import face_recognition
from fastapi import APIRouter, Depends, HTTPException, Form, UploadFile, File
from sqlalchemy.orm import Session
from typing import List, Optional

from app.database import get_db
from app.models import Student
from app.auth import admin_or_teacher, hash_password
from app.schemas import StudentCreate, StudentUpdate, StudentOut

router = APIRouter(
    prefix="/admin/students",
    tags=["Admin - Students"]
)

from app.utils.image_utils import resolve_profile_image

# Helper for Encodings
def process_face_encoding(file_path: str, student_id: int):
    try:
        image = face_recognition.load_image_file(file_path)
        encodings = face_recognition.face_encodings(image)
        if encodings:
            encoding = encodings[0]
            # Save encoding
            enc_dir = os.path.join("images", "encodings")
            os.makedirs(enc_dir, exist_ok=True)
            np.save(os.path.join(enc_dir, f"{student_id}.npy"), encoding)
            return encoding
    except Exception as e:
        print(f"Error processing face: {e}")
    return None

@router.post("/", response_model=StudentOut)
def add_student(
    name: str = Form(...),
    roll_no: str = Form(...),
    program: Optional[str] = Form(None),
    department: str = Form(...),
    year: int = Form(...),
    semester: int = Form(None),
    section: str = Form(...),
    email: Optional[str] = Form(None),     # Student Email
    mobile: Optional[str] = Form(None),    # Student Mobile
    guardian_name: Optional[str] = Form(None),
    guardian_email: Optional[str] = Form(None),
    guardian_mobile: Optional[str] = Form(None),
    image: UploadFile = File(None),
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    from app.models import User
    existing = db.query(Student).filter(Student.roll_no == roll_no).first()
    if existing:
        raise HTTPException(status_code=400, detail="Roll number already exists")
    
    import re
    # Extract ID from roll number (e.g., 23cse706 -> 706)
    match = re.search(r"(\d+)$", roll_no)
    if not match:
        raise HTTPException(status_code=400, detail="Invalid Roll No format. Must end with digits.")
    
    extracted_id = int(match.group(1))

    # Check if ID collision
    id_exists = db.query(Student).filter(Student.id == extracted_id).first()
    if id_exists:
         raise HTTPException(status_code=400, detail=f"Student ID {extracted_id} (from Roll {roll_no}) already exists.")

    existing_user = db.query(User).filter(User.username == name).first()
    if existing_user:
        raise HTTPException(status_code=400, detail=f"Username '{name}' is already taken. Please use a unique name.")

    # Create User account for student
    new_user = User(
        username=name, # Username is Student Name
        password=hash_password(roll_no), # Initial password is roll no (hashed)
        role="student",
        email=email,
        mobile=mobile
    )
    db.add(new_user)
    db.flush() # Get user id

    student = Student(
        id=extracted_id,
        user_id=new_user.id,
        name=name,
        roll_no=roll_no,
        program=program,
        department=department,
        year=year,
        semester=semester,
        section=section,
        guardian_name=guardian_name,
        guardian_email=guardian_email,
        guardian_mobile=guardian_mobile
    )
    
    db.add(student)
    db.commit()
    db.refresh(student)

    # Handle Image Upload
    if image:
        try:
            # Save Image
            img_dir = os.path.join("images", "students")
            os.makedirs(img_dir, exist_ok=True)
            file_path = os.path.join(img_dir, f"{roll_no}.jpg")
            
            with open(file_path, "wb") as buffer:
                shutil.copyfileobj(image.file, buffer)
            
            # Update Profile Image Path
            new_user.profile_image = f"/images/students/{roll_no}.jpg"
            db.commit()

            # Process Encoding
            new_encoding = process_face_encoding(file_path, extracted_id)
            
            # Update known faces immediately in memory
            if new_encoding is not None:
                from app.face_recognition.recognize import add_or_update_face
                add_or_update_face(extracted_id, new_encoding)


        except Exception as e:
            print(f"Failed to save image: {e}")

    # Populate response fields
    student_out = StudentOut.model_validate(student)
    if student.user:
        student_out.profile_image = resolve_profile_image(student.user, student)

    return student_out
@router.get("/")
def get_students(
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    from app.models import User
    # Join with User to include password for admin visibility
    query = db.query(Student, User.password).outerjoin(User, Student.user_id == User.id)

    if user["role"] == "teacher":
        from app.models import SectionMetadata, User as UserModel
        from sqlalchemy import or_, and_

        # Fetch full user object to check HOD status (not in token)
        db_user = db.query(UserModel).filter(UserModel.id == user["id"]).first()
        if not db_user:
            return []

        # 1. HOD Logic
        hod_filter = None
        if db_user.is_hod and db_user.hod_department:
            # User is HOD -> Access to entire Department
            hod_filter = (Student.department == db_user.hod_department)
        
        # 2. Class Teacher Logic
        ct_filter = None
        sections = db.query(SectionMetadata).filter(SectionMetadata.class_teacher_id == user["id"]).all()
        if sections:
             ct_conditions = []
             for s in sections:
                 # Match Department, Year, Section
                 ct_conditions.append(and_(
                     Student.department == s.department,
                     Student.year == s.year,
                     Student.section == s.section
                 ))
             if ct_conditions:
                 ct_filter = or_(*ct_conditions)
        
        # Combine Permissions
        if hod_filter is not None and ct_filter is not None:
            # Grant access if EITHER HOD matches OR Class Teacher matches
            query = query.filter(or_(hod_filter, ct_filter))
        elif hod_filter is not None:
             query = query.filter(hod_filter)
        elif ct_filter is not None:
             query = query.filter(ct_filter)
        else:
             # Neither HOD nor Class Teacher -> No Access
             return []

    results = query.all()
    out = []
    for s, p in results:
        d = s.__dict__.copy()
        d["password"] = p # Admin can see the password
        if s.user:
            d["email"] = s.user.email
            d["mobile"] = s.user.mobile
            d["profile_image"] = resolve_profile_image(s.user, s)
        else:
            d["email"] = None
            d["mobile"] = None
            d["profile_image"] = None
        out.append(d)
    return out

@router.get("/{student_id}", response_model=StudentOut)
def get_student(
    student_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")
    
    # Attach user fields to the response model instance
    # Attach user fields to the response model instance
    student_out = StudentOut.model_validate(student)
    if student.user:
        student_out.profile_image = resolve_profile_image(student.user, student)
    
    return student_out

@router.put("/{student_id}", response_model=StudentOut)
def update_student(
    student_id: int,
    name: Optional[str] = Form(None),
    roll_no: Optional[str] = Form(None),
    program: Optional[str] = Form(None),
    department: Optional[str] = Form(None),
    year: Optional[int] = Form(None),
    section: Optional[str] = Form(None),
    email: Optional[str] = Form(None),     
    mobile: Optional[str] = Form(None),    
    guardian_name: Optional[str] = Form(None),
    guardian_email: Optional[str] = Form(None),
    guardian_mobile: Optional[str] = Form(None),
    image: UploadFile = File(None),
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    from app.models import User
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")

    # Update Student Fields
    # Helper to handle empty strings -> None
    def clean_input(val):
        return None if val == "" else val

    # Clean inputs
    email = clean_input(email)
    mobile = clean_input(mobile)
    guardian_name = clean_input(guardian_name)
    guardian_email = clean_input(guardian_email)
    guardian_mobile = clean_input(guardian_mobile)
    
    # Update Student Fields
    if name is not None: student.name = name
    if roll_no is not None: student.roll_no = roll_no
    if program is not None: student.program = program
    if department is not None: student.department = department
    if year is not None: student.year = year
    if section is not None: student.section = section
    
    # Always update these, treating None as "clear" if intended, 
    # but since Form(None) is default, we only update if it's in the form? 
    # Actually, with Form(None), if the field is missing from request, it's None.
    # If the user sends "", it's "".
    # We should only update if it is explicitly provided (not None).
    # But for "clearing", the client must send "".
    
    if guardian_name is not None: student.guardian_name = guardian_name
    if guardian_email is not None: student.guardian_email = guardian_email
    if guardian_mobile is not None: student.guardian_mobile = guardian_mobile

    print(f"DEBUG: update_student id={student_id}, email={email}, mobile={mobile}")

    # Update User Fields
    if student.user:
        print(f"DEBUG: Found user {student.user.id}")
        if email is not None: 
            print(f"DEBUG: Updating email to {email}")
            student.user.email = email
        if mobile is not None: 
            print(f"DEBUG: Updating mobile to {mobile}")
            student.user.mobile = mobile
        print(f"DEBUG: Current username={repr(student.user.username)}, New name={repr(name)}")
        if name and student.user.username != name:
            # Check for duplicate
            # from app.models import User # imported at top
            existing_user = db.query(User).filter(User.username == name).first()
            if existing_user:
                 print(f"DEBUG: Conflicting user: ID={existing_user.id}, Username={repr(existing_user.username)}")
                 raise HTTPException(status_code=400, detail=f"Username '{name}' is already taken.")
            student.user.username = name 
    else:
        print("DEBUG: Student has no user!")
    
    # Handle Image Update
    if image:
        try:
            # Save Image (Use existing roll_no if not updated, or new roll_no)
            current_roll = student.roll_no
            img_dir = os.path.join("images", "students")
            os.makedirs(img_dir, exist_ok=True)
            file_path = os.path.join(img_dir, f"{current_roll}.jpg")
            
            with open(file_path, "wb") as buffer:
                shutil.copyfileobj(image.file, buffer)
            
            if student.user:
                student.user.profile_image = f"/images/students/{current_roll}.jpg"

            # Process Encoding
            new_encoding = process_face_encoding(file_path, student.id)
            
            # Update known faces immediately
            if new_encoding is not None:
                from app.face_recognition.recognize import add_or_update_face
                add_or_update_face(student.id, new_encoding)


        except Exception as e:
            print(f"Failed to update image: {e}")

    db.commit()
    db.refresh(student)
    
    # Ensure fields are set for response
    # Ensure fields are set for response
    student_out = StudentOut.model_validate(student)
    if student.user:
        student_out.profile_image = resolve_profile_image(student.user, student)
        
    return student_out

@router.delete("/{student_id}")
def delete_student(
    student_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    from app.models import User, LeaveRequest, Attendance
    student = db.query(Student).filter(Student.id == student_id).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")

    user_id = student.user_id

    # Clear references or delete related data
    db.query(LeaveRequest).filter(LeaveRequest.student_id == student_id).delete()
    db.query(Attendance).filter(Attendance.student_id == student_id).delete()

    db.delete(student)
    
    if user_id:
        u = db.query(User).filter(User.id == user_id).first()
        if u:
            db.delete(u)

    db.commit()
    return {"message": "Student and user account deleted successfully"}

