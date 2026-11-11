from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import OAuth2PasswordRequestForm
from pydantic import BaseModel
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import User
from app.auth import hash_password, verify_password, create_access_token, get_current_user

router = APIRouter(prefix="/auth", tags=["Auth"])


@router.post("/register")
def register_user(username: str, password: str, role: str, db: Session = Depends(get_db)):
    role = role.lower()
    if role not in ["admin", "teacher", "student"]:
        raise HTTPException(status_code=400, detail="Invalid role")

    user = db.query(User).filter(User.username == username).first()
    if user:
        raise HTTPException(status_code=400, detail="User already exists")

    new_user = User(
        username=username,
        password=hash_password(password),
        role=role
    )

    db.add(new_user)
    db.commit()
    return {"message": "User registered successfully"}



@router.post("/login")
def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db)
):
    user = db.query(User).filter(
        User.username == form_data.username
    ).first()

    if not user or not verify_password(form_data.password, user.password):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    token = create_access_token({
        "sub": user.username,
        "role": user.role,
        "id": user.id
    })

    # Additional specific logic for Teachers
    is_hod = False
    is_class_teacher = False

    if user.role == "teacher":
        if user.is_hod: is_hod = True
        
        # Check Class Teacher status
        from app.models import SectionMetadata
        ct = db.query(SectionMetadata).filter(SectionMetadata.class_teacher_id == user.id).first()
        if ct: is_class_teacher = True

    return {
        "access_token": token,
        "token_type": "bearer",
        "username": user.username,
        "role": user.role,
        "is_hod": is_hod,
        "is_class_teacher": is_class_teacher
    }

@router.get("/profile", response_model=None) # Start with no strict response model to debug, or use UserProfile
def get_profile(
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    from app.models import Student, SectionMetadata
    import os
    from datetime import date

    user = db.query(User).filter(User.id == current_user["id"]).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Determine dynamic position string
    position_parts = []
    
    def to_ordinal(n):
        if n is None: return ""
        try:
            n = int(n)
        except:
             return str(n)
             
        if 11 <= (n % 100) <= 13: return f"{n}th"
        suffix = {1: 'st', 2: 'nd', 3: 'rd'}.get(n % 10, 'th')
        return f"{n}{suffix}"

    def get_batch_str(year):
         if not year: return ""
         current_year = date.today().year
         start_year = current_year - int(year)
         end_year = start_year + 4
         return f"({str(start_year)[-2:]}-{str(end_year)[-2:]})"
    
    # 1. Base Position from DB (e.g. Asst. Professor)
    if user.position:
        position_parts.append(user.position)
        
    # 2. Check for HOD
    if user.is_hod:
         hod_text = f"HOD - {user.hod_department}" if user.hod_department else "HOD"
         # Defaulting to Year 3 to match user request "23-27" for HOD
         batch_str = get_batch_str(3)
         position_parts.append(f"{hod_text} {batch_str}")
         
    # 3. Check for Class Teacher
    if user.role == "teacher":
        section_meta = db.query(SectionMetadata).filter(SectionMetadata.class_teacher_id == user.id).first()
        if section_meta:
             yr_ord = to_ordinal(section_meta.year)
             batch_str = get_batch_str(section_meta.year)
             position_parts.append(f"Class Teacher ({yr_ord} - {section_meta.section}) {batch_str}")
             
    if position_parts:
        position_display = " & ".join(position_parts)
    elif user.role == "teacher":
        position_display = "Faculty"
    else:
        position_display = None 
        if user.role == "student":
             position_display = "Student"


    profile_data = {
        "id": user.id,
        "username": user.username,
        "name": user.full_name,
        "role": user.role,
        "mobile": getattr(user, 'mobile', None),
        "email": getattr(user, 'email', None),
        "address": getattr(user, 'address', None),
        "qualification": getattr(user, 'qualification', None),
        "position": position_display,
        "profile_image": user.profile_image,
        "department": getattr(user, 'department', None)
    }

    # Populate Student Specific Fields
    if user.role == "student":
        student = db.query(Student).filter(Student.user_id == user.id).first()
        if student:
            profile_data.update({
                "name": student.name,
                "department": student.department,
                "year": student.year,
                "semester": student.semester,
                "section": student.section,
                "roll_no": student.roll_no,
                "program": student.program,
                "student_id": student.id,
                "guardian_name": student.guardian_name,
                "guardian_email": student.guardian_email,
                "guardian_mobile": student.guardian_mobile,
                "blood_group": student.blood_group
            })

    # Image Fallback Logic (if not in DB)
    from app.utils.image_utils import resolve_profile_image
    
    # We need to construct a Student-like object or pass None if not a student
    student_obj = None
    if user.role == "student":
        student_obj = db.query(Student).filter(Student.user_id == user.id).first()
        
    resolved_image = resolve_profile_image(user, student_obj)
    
    # Update profile_data ONLY if we resolved something (or if we want to overwrite None with None)
    profile_data["profile_image"] = resolved_image
            
    return profile_data

# ---------------- FORGOT PASSWORD ----------------
import random
import string

class ForgotPasswordRequest(BaseModel):
    username: str

class VerifyOtpRequest(BaseModel):
    username: str
    otp: str

class ResetPasswordRequest(BaseModel):
    username: str
    otp: str
    new_password: str

@router.post("/forgot-password")
def forgot_password(req: ForgotPasswordRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == req.username).first()
    if not user:
        # For security, don't reveal if user exists, just pretend it worked
        # But for development/this project, returning 404 helps debug
        raise HTTPException(status_code=404, detail="User not found")
    
    # Generate 6-digit OTP
    otp = ''.join(random.choices(string.digits, k=6))
    
    # Save to user record
    user.otp = otp
    db.commit()
    
    # Simulate sending email
    print(f"==================================================")
    print(f" OTP for {user.username}: {otp}")
    print(f"==================================================")
    
    return {"message": "OTP sent to your registered email (Console for now)"}

@router.post("/verify-otp")
def verify_otp(req: VerifyOtpRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == req.username).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if not user.otp or user.otp != req.otp:
        raise HTTPException(status_code=400, detail="Invalid OTP")
        
    return {"message": "OTP Verified"}

@router.post("/reset-password-with-otp")
def reset_password_with_otp(req: ResetPasswordRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == req.username).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # Verify OTP again to be safe
    if not user.otp or user.otp != req.otp:
        raise HTTPException(status_code=400, detail="Invalid or expired OTP")
    
    # Reset password
    user.password = hash_password(req.new_password)
    user.otp = None # Clear OTP after use
    db.commit()
    
    return {"message": "Password reset successfully"}

from app.schemas import FCMTokenUpdate
from app.auth_dependencies import get_current_active_user

@router.post("/update-fcm-token")
def update_fcm_token(
    fcm_data: FCMTokenUpdate,
    current_user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    current_user.fcm_token = fcm_data.fcm_token
    db.commit()
    return {"message": "FCM token updated successfully"}
