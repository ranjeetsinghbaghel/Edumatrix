from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import LeaveRequest, Student
from app.auth import student_only

router = APIRouter(prefix="/student/leave", tags=["Student - Leave"])

from pydantic import BaseModel

class LeaveCreate(BaseModel):
    reason: str

@router.post("/apply")
def apply_leave(
    req: LeaveCreate,
    db: Session = Depends(get_db),
    user=Depends(student_only)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        student = db.query(Student).filter(Student.name == user["username"]).first()

    if not student:
        return {"error": "Student not found"}

    leave = LeaveRequest(
        student_id=student.id,
        reason=req.reason
    )

    db.add(leave)
    db.commit()

    return {"message": "Leave request submitted"}

@router.get("/my-leaves")
def my_leave_requests(
    db: Session = Depends(get_db),
    user=Depends(student_only)
):
    student = db.query(Student).filter(Student.user_id == user["id"]).first()
    if not student:
        student = db.query(Student).filter(Student.name == user["username"]).first()

    if not student:
        return []

    leaves = db.query(LeaveRequest).filter(
        LeaveRequest.student_id == student.id
    ).order_by(LeaveRequest.id.desc()).all()

    return [
        {
            "id": l.id,
            "reason": l.reason,
            "date": l.date,
            "status": l.status
        }
        for l in leaves
    ]

