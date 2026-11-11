from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from app.database import get_db
from app.models import Subject, User
from app.auth import admin_only
from app.schemas import SubjectCreate, SubjectUpdate, SubjectOut

router = APIRouter(prefix="/admin/subjects", tags=["Admin - Subjects"])

@router.post("/", response_model=SubjectOut)
def add_subject(
    subject_data: SubjectCreate,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    existing = db.query(Subject).filter(Subject.id == subject_data.id).first()
    if existing:
        raise HTTPException(status_code=400, detail="Subject already exists")

    if subject_data.teacher_id:
        teacher = db.query(User).filter(User.id == subject_data.teacher_id, User.role == "teacher").first()
        if not teacher:
            raise HTTPException(status_code=404, detail="Teacher not found")

    subject = Subject(**subject_data.dict())
    db.add(subject)
    db.commit()
    db.refresh(subject)
    return subject

@router.get("/", response_model=List[SubjectOut])
def get_subjects(
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    return db.query(Subject).all()

@router.put("/{subject_id}", response_model=SubjectOut)
def update_subject(
    subject_id: str,
    subject_data: SubjectUpdate,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    subject = db.query(Subject).filter(Subject.id == subject_id).first()
    if not subject:
        raise HTTPException(status_code=404, detail="Subject not found")

    if subject_data.teacher_id:
        teacher = db.query(User).filter(User.id == subject_data.teacher_id, User.role == "teacher").first()
        if not teacher:
            raise HTTPException(status_code=404, detail="Teacher not found")

    for key, value in subject_data.dict().items():
        setattr(subject, key, value)

    db.commit()
    db.refresh(subject)
    return subject

@router.delete("/{subject_id}")
def delete_subject(
    subject_id: str,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    from app.models import ClassSchedule, Attendance
    subject = db.query(Subject).filter(Subject.id == subject_id).first()
    if not subject:
        raise HTTPException(status_code=404, detail="Subject not found")

    # Clear code-based references in other tables
    db.query(ClassSchedule).filter(ClassSchedule.subject == subject.id).delete()
    db.query(Attendance).filter(Attendance.subject == subject.id).delete()

    db.delete(subject)
    db.commit()
    return {"message": "Subject deleted and schedules cleared"}

