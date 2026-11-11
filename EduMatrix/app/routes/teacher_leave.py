from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import LeaveRequest, Student, SectionMetadata, User
from app.auth import teacher_only
from typing import List
from pydantic import BaseModel

class LeaveActionRequest(BaseModel):
    leave_id: int
    action: str # "approve" or "reject"

class HodActionRequest(BaseModel):
    leave_id: int
    approve: bool

router = APIRouter(prefix="/teacher/leave", tags=["Teacher - Leave Management"])

@router.get("/assigned-section-requests")
def get_section_leave_requests(
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    # Get sections where this teacher is a Class Teacher
    sections = db.query(SectionMetadata).filter(SectionMetadata.class_teacher_id == user["id"]).all()
    if not sections:
        return []

    all_leaves = []
    for section in sections:
        leaves = db.query(LeaveRequest, Student).join(Student, LeaveRequest.student_id == Student.id).filter(
            Student.department == section.department,
            Student.year == section.year,
            Student.section == section.section,
            LeaveRequest.status == "Pending_Class_Teacher"
        ).order_by(LeaveRequest.id.desc()).all()
        
        for l, s in leaves:
            all_leaves.append({
                "id": l.id,
                "student_name": s.name,
                "roll_no": s.roll_no,
                "reason": l.reason,
                "date": l.date,
                "status": l.status
            })
    
    return all_leaves

@router.post("/section-action")
def section_action(
    req: LeaveActionRequest,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    leave = db.query(LeaveRequest).filter(LeaveRequest.id == req.leave_id).first()
    if not leave:
        raise HTTPException(status_code=404, detail="Leave not found")
    
    # Optional: Verify user is the class teacher
    
    print(f"DEBUG: Processing leave {req.leave_id} action {req.action}")
    if req.action == "approve":
        leave.status = "Pending_HOD"
    elif req.action == "reject":
        leave.status = "Rejected_By_Class_Teacher"
    else:
        raise HTTPException(status_code=400, detail="Invalid action")

    db.commit()
    print(f"DEBUG: Updated status to {leave.status}")
    return {"message": f"Leave {req.action}d successfully"}

@router.get("/hod-requests")
def get_hod_leave_requests(
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    # Check if teacher is HOD
    me = db.query(User).filter(User.id == user["id"]).first()
    if not me or not me.is_hod:
        raise HTTPException(status_code=403, detail="HOD access required")
    
    # Get leaves forwarded to HOD in their department (Pending or Processed by HOD)
    # HOD should not see 'Pending_Class_Teacher' or 'Rejected_By_Class_Teacher' usually, as they are not candidates for HOD action yet (or ever).
    
    leaves = db.query(LeaveRequest, Student).join(Student, LeaveRequest.student_id == Student.id).filter(
        Student.department == me.hod_department,
        LeaveRequest.status == "Pending_HOD"
    ).order_by(LeaveRequest.id.desc()).all()
    
    return [
        {
            "id": l.id,
            "student_name": s.name,
            "roll_no": s.roll_no,
            "reason": l.reason,
            "date": l.date,
            "status": l.status
        }
        for l, s in leaves
    ]

@router.post("/hod-action")
def hod_action(
    req: HodActionRequest,
    db: Session = Depends(get_db),
    user=Depends(teacher_only)
):
    print(f"DEBUG: HOD Action for leave {req.leave_id} approve={req.approve}")
    me = db.query(User).filter(User.id == user["id"]).first()
    if not me or not me.is_hod:
        raise HTTPException(status_code=403, detail="HOD access required")
        
    leave = db.query(LeaveRequest).filter(LeaveRequest.id == req.leave_id).first()
    if not leave:
        raise HTTPException(status_code=404, detail="Leave not found")
        
    leave.status = "Approved_By_HOD" if req.approve else "Rejected_By_HOD"
    db.commit()
    print(f"DEBUG: HOD Updated status to {leave.status}")
    return {"message": f"Leave {leave.status} successfully"}
