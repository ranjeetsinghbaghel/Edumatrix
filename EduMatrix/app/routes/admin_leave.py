from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import LeaveRequest
from app.auth import admin_only
from sqlalchemy import and_
from app.models import LeaveRequest

router = APIRouter(prefix="/admin/leaves", tags=["Admin Leaves"])

@router.put("/approve/{leave_id}")
def approve_leave(
    leave_id: int,
    db: Session = Depends(get_db),
    user=Depends(admin_only)
):
    leave = db.query(LeaveRequest).filter(
        LeaveRequest.id == leave_id
    ).first()

    if not leave:
        return {"error": "Leave not found"}

    leave.status = "Approved"
    db.commit()

    return {"message": "Leave approved successfully"}

    