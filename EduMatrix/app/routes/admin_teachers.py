from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from app.database import get_db
from app.models import User
from app.auth import admin_only, hash_password 
from app.schemas import TeacherCreate, TeacherUpdate, TeacherOut

router = APIRouter(
    prefix="/admin/teachers",
    tags=["Admin - Teachers"]
)

from fastapi import File, Form, UploadFile
import os
import shutil

from app.face_recognition.utils import save_face_encoding
from app.utils.image_utils import resolve_profile_image

@router.post("/", response_model=TeacherOut)
def add_teacher(
    username: str = Form(...),
    full_name: str = Form(...),
    password: str = Form(...),
    email: str = Form(None),
    mobile: str = Form(None),
    address: str = Form(None),
    qualification: str = Form(None),
    file: UploadFile = File(None),
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    existing = db.query(User).filter(User.username == username).first()
    if existing:
        raise HTTPException(status_code=400, detail="Teacher (User) already exists")

    teacher = User(
        username=username,
        full_name=full_name,
        email=email,
        mobile=mobile,
        address=address,
        qualification=qualification,
        password=hash_password(password),
        role="teacher"
    )

    db.add(teacher)
    db.flush() # Get ID if needed, but we use username

    # Handle Image Upload
    if file:
        try:
            os.makedirs("images/registered", exist_ok=True)
            # We save as username.jpg so auth_routes generic logic picks it up
            image_path = f"images/registered/{username}.jpg"
            with open(image_path, "wb") as buffer:
                shutil.copyfileobj(file.file, buffer)
            
            teacher.profile_image = f"/{image_path}"
            
            # Generate Encoding to verify face
            # We use username as identifier. Note: recognize.py currently skips non-numeric filenames, 
            # so this won't break student attendance but ensures face validation.
            save_face_encoding(username, image_path)
            
        except Exception as e:
            # If face detection fails or other io error, we might want to rollback or just error
            db.rollback()
            # Clean up the file if it was created?
            if os.path.exists(image_path):
                os.remove(image_path)
            raise HTTPException(status_code=400, detail=f"Image processing failed: {str(e)}")

    db.commit()
    db.refresh(teacher)

    teacher_out = TeacherOut.model_validate(teacher)
    teacher_out.profile_image = resolve_profile_image(teacher)
    return teacher_out

@router.get("/", response_model=List[TeacherOut])
def get_teachers(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    teachers = db.query(User).filter(User.role == "teacher").all()
    results = []
    for t in teachers:
        t_out = TeacherOut.model_validate(t)
        t_out.profile_image = resolve_profile_image(t)
        results.append(t_out)
    return results

@router.put("/{teacher_id}", response_model=TeacherOut)
def update_teacher(
    teacher_id: int,
    username: str = Form(None),
    full_name: str = Form(None),
    password: str = Form(None),
    email: str = Form(None),
    mobile: str = Form(None),
    address: str = Form(None),
    qualification: str = Form(None),
    file: UploadFile = File(None),
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    teacher = db.query(User).filter(User.id == teacher_id, User.role == "teacher").first()
    if not teacher:
        raise HTTPException(status_code=404, detail="Teacher not found")

    if username: teacher.username = username
    if full_name: teacher.full_name = full_name
    if email: teacher.email = email
    if mobile: teacher.mobile = mobile
    if address: teacher.address = address
    if qualification: teacher.qualification = qualification

    if password:
        teacher.password = hash_password(password)

    if file:
        try:
            os.makedirs("images/registered", exist_ok=True)
            # Use existing username or updated one
            u_name = username if username else teacher.username
            image_path = f"images/registered/{u_name}.jpg"
            with open(image_path, "wb") as buffer:
                 shutil.copyfileobj(file.file, buffer)
            
            teacher.profile_image = f"/{image_path}"
            
            # Verify and save encoding
            save_face_encoding(u_name, image_path)
            
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Image processing failed: {str(e)}")

    db.commit()
    db.refresh(teacher)
    
    teacher_out = TeacherOut.model_validate(teacher)
    teacher_out.profile_image = resolve_profile_image(teacher)
    return teacher_out

@router.post("/assign-hod")
def assign_hod(
    teacher_id: int,
    department: str,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    teacher = db.query(User).filter(User.id == teacher_id, User.role == "teacher").first()
    if not teacher:
        raise HTTPException(status_code=404, detail="Teacher not found")
    
    teacher.is_hod = 1
    teacher.hod_department = department
    db.commit()
    return {"message": f"Teacher {teacher.username} assigned as HOD of {department}"}

@router.post("/assign-class-teacher")
def assign_class_teacher(
    teacher_id: int,
    department: str,
    year: int,
    semester: int,
    section: str,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    from app.models import SectionMetadata
    existing = db.query(SectionMetadata).filter(
        SectionMetadata.department == department,
        SectionMetadata.year == year,
        SectionMetadata.semester == semester,
        SectionMetadata.section == section
    ).first()
    
    if existing:
        existing.class_teacher_id = teacher_id
    else:
        meta = SectionMetadata(
            department=department,
            year=year,
            semester=semester,
            section=section,
            class_teacher_id=teacher_id
        )
        db.add(meta)
    
    db.commit()
    return {"message": "Class Teacher assigned successfully"}

@router.delete("/{teacher_id}")
def delete_teacher(
    teacher_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    teacher = db.query(User).filter(User.id == teacher_id, User.role == "teacher").first()
    if not teacher:
        raise HTTPException(status_code=404, detail="Teacher not found")

    # Clear references in other tables to avoid foreign key constraint errors
    from app.models import Subject, ClassSchedule, SectionMetadata
    
    db.query(Subject).filter(Subject.teacher_id == teacher_id).update({"teacher_id": None})
    db.query(ClassSchedule).filter(ClassSchedule.teacher_id == teacher_id).update({"teacher_id": None})
    db.query(SectionMetadata).filter(SectionMetadata.class_teacher_id == teacher_id).update({"class_teacher_id": None})

    db.delete(teacher)
    db.commit()
    return {"message": "Teacher deleted successfully"}

