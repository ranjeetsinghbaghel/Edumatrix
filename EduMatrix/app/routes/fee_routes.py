from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth_dependencies import get_current_active_user
from app.models import User, Student
from app.models_advanced import StudentFee, FeePaymentTransaction, ParentPaymentRequest
from pydantic import BaseModel
from typing import List
import random
from datetime import datetime

router = APIRouter(prefix="/fees", tags=["Smart Fee Portal"])

# --- Schemas ---
class FeeDuesOut(BaseModel):
    tuition_fee_due: float
    hostel_fee_due: float
    exam_fee_due: float
    library_fine_due: float
    total_due: float
    parent_name: str
    parent_phone: str

class PayRequest(BaseModel):
    category: str # "Tuition", "Hostel", "Exam", "Library"
    amount: float
    payment_method: str # "UPI", "CARD", "NET_BANKING"

class PayResponse(BaseModel):
    transaction_id: str
    amount: float
    category: str
    payment_method: str
    status: str
    paid_at: str
    message: str

class TransactionOut(BaseModel):
    transaction_id: str
    amount: float
    category: str
    payment_method: str
    status: str
    paid_at: str

# --- Helper ---
def get_student_id(user_id: int, db: Session):
    student = db.query(Student).filter(Student.user_id == user_id).first()
    if not student:
        # Create default student record if it does not exist for the user (for local testing/admin fallback)
        user = db.query(User).filter(User.id == user_id).first()
        student = Student(
            user_id=user_id,
            name=user.full_name if (user and user.full_name) else "Ranjeet Kumar",
            roll_no=f"GIET-2026-{user_id:04d}",
            program="B.Tech",
            department="Computer Science",
            year=4,
            semester=7,
            section="A",
            guardian_name="Ramesh Kumar",
            guardian_mobile="+91 9988776655",
            guardian_email="ramesh@example.com"
        )
        db.add(student)
        db.commit()
        db.refresh(student)
    return student.id

# --- Routes ---

@router.get("/dues", response_model=FeeDuesOut)
def get_dues(
    user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = get_student_id(user.id, db)
    
    # Query parent name and phone from the student profile
    student = db.query(Student).filter(Student.id == student_id).first()
    parent_name = student.guardian_name if (student and student.guardian_name) else "Ramesh Kumar"
    parent_phone = student.guardian_mobile if (student and student.guardian_mobile) else "+91 9988776655"

    fee = db.query(StudentFee).filter(StudentFee.student_id == student_id).first()
    if not fee:
        # Create default dues
        fee = StudentFee(
            student_id=student_id,
            tuition_fee_due=85000.0,
            hostel_fee_due=30000.0,
            exam_fee_due=2500.0,
            library_fine_due=150.0
        )
        db.add(fee)
        db.commit()
        db.refresh(fee)
        
    total = fee.tuition_fee_due + fee.hostel_fee_due + fee.exam_fee_due + fee.library_fine_due
    return FeeDuesOut(
        tuition_fee_due=fee.tuition_fee_due,
        hostel_fee_due=fee.hostel_fee_due,
        exam_fee_due=fee.exam_fee_due,
        library_fine_due=fee.library_fine_due,
        total_due=total,
        parent_name=parent_name,
        parent_phone=parent_phone
    )

@router.post("/pay", response_model=PayResponse)
def pay_fee(
    req: PayRequest,
    user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = get_student_id(user.id, db)
    fee = db.query(StudentFee).filter(StudentFee.student_id == student_id).first()
    if not fee:
        fee = StudentFee(
            student_id=student_id,
            tuition_fee_due=85000.0,
            hostel_fee_due=30000.0,
            exam_fee_due=2500.0,
            library_fine_due=150.0
        )
        db.add(fee)
        db.commit()
        db.refresh(fee)
        
    category_lower = req.category.strip().lower()
    
    # Process payment with tolerance for floating point representations
    if "tuition" in category_lower:
        if (fee.tuition_fee_due + 0.5) < req.amount:
            raise HTTPException(status_code=400, detail="Payment amount exceeds tuition due")
        fee.tuition_fee_due = max(0.0, round(fee.tuition_fee_due - req.amount, 2))
    elif "hostel" in category_lower:
        if (fee.hostel_fee_due + 0.5) < req.amount:
            raise HTTPException(status_code=400, detail="Payment amount exceeds hostel due")
        fee.hostel_fee_due = max(0.0, round(fee.hostel_fee_due - req.amount, 2))
    elif "exam" in category_lower:
        if (fee.exam_fee_due + 0.5) < req.amount:
            raise HTTPException(status_code=400, detail="Payment amount exceeds exam due")
        fee.exam_fee_due = max(0.0, round(fee.exam_fee_due - req.amount, 2))
    elif "library" in category_lower:
        if (fee.library_fine_due + 0.5) < req.amount:
            raise HTTPException(status_code=400, detail="Payment amount exceeds library fine due")
        fee.library_fine_due = max(0.0, round(fee.library_fine_due - req.amount, 2))
    else:
        raise HTTPException(status_code=400, detail="Invalid fee category")
        
    fee.updated_at = datetime.utcnow()
    
    # Save transaction
    tx_id = f"TXN-{random.randint(100000, 999999)}"
    transaction = FeePaymentTransaction(
        student_id=student_id,
        amount=req.amount,
        category=req.category,
        transaction_id=tx_id,
        payment_method=req.payment_method,
        status="SUCCESS",
        paid_at=datetime.utcnow()
    )
    db.add(transaction)
    db.commit()
    
    return PayResponse(
        transaction_id=tx_id,
        amount=req.amount,
        category=req.category,
        payment_method=req.payment_method,
        status="SUCCESS",
        paid_at=transaction.paid_at.isoformat(),
        message="Payment processed successfully! Your digital receipt has been generated."
    )

@router.get("/transactions", response_model=List[TransactionOut])
def get_transactions(
    user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = get_student_id(user.id, db)
    txs = db.query(FeePaymentTransaction).filter(FeePaymentTransaction.student_id == student_id).order_by(FeePaymentTransaction.paid_at.desc()).all()
    
    return [
        TransactionOut(
            transaction_id=t.transaction_id,
            amount=t.amount,
            category=t.category,
            payment_method=t.payment_method,
            status=t.status,
            paid_at=t.paid_at.isoformat()
        ) for t in txs
    ]

class ParentRequestInput(BaseModel):
    category: str
    amount: float
    parent_name: str
    parent_phone: str

@router.post("/request-parent")
def request_parent_payment(
    req: ParentRequestInput,
    user: User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = get_student_id(user.id, db)
    student = db.query(Student).filter(Student.id == student_id).first()
    student_name = student.name if student else (user.full_name or "Student")
    roll_no = student.roll_no if student else user.username
    
    # Save the request in the database
    db_req = ParentPaymentRequest(
        student_id=student_id,
        category=req.category,
        amount=req.amount,
        parent_name=req.parent_name,
        parent_phone=req.parent_phone,
        status="PENDING",
        created_at=datetime.utcnow()
    )
    db.add(db_req)
    
    # Create an in-app notification record
    try:
        from app.models import Notification
        notif = Notification(
            user_id=user.id,
            title=f"Fee Request Dispatched: ₹{req.amount:,.2f}",
            message=f"A payment request of ₹{req.amount:,.2f} for {req.category} Fee was successfully sent to {req.parent_name} ({req.parent_phone}).",
            type="FEE_REQUEST",
            created_at=datetime.utcnow()
        )
        db.add(notif)
    except Exception as e:
        print(f"Error creating notification record: {e}", flush=True)

    db.commit()

    # Trigger Firebase push notification and simulated SMS gateway dispatch to the phone number
    try:
        from app import firebase_utils
        if user.fcm_token:
            firebase_utils.send_multicast_notification(
                tokens=[user.fcm_token],
                title=f"EduMatrix: Fee Request Dispatched",
                body=f"Fee request of ₹{req.amount:,.2f} for {student_name} ({roll_no}) sent to {req.parent_phone}.",
                data={"type": "FEE_REQUEST", "amount": str(req.amount), "phone": req.parent_phone}
            )
        
        # Log simulated SMS gateway dispatch
        print(f"[SMS/WHATSAPP GATEWAY] Notification dispatched to {req.parent_phone}: 'Dear {req.parent_name}, GIET University has generated a fee payment request for {student_name} ({roll_no}) of Rs. {req.amount:,.2f} for {req.category} Fee. Please pay via EduMatrix portal.'", flush=True)
    except Exception as e:
        print(f"Fee notification dispatch error: {e}", flush=True)

    return {
        "message": f"Successfully sent payment request for {req.category} (₹{req.amount:,.2f}) to {req.parent_name} at {req.parent_phone}! Notification dispatched."
    }
