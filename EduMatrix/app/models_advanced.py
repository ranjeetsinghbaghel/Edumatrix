from sqlalchemy import Column, Integer, String, Float, Boolean, ForeignKey, DateTime, JSON
from sqlalchemy.orm import relationship
from app.database import Base
from datetime import datetime

class MockTestAttempt(Base):
    __tablename__ = "mock_test_attempts"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    category = Column(String, index=True) # SUBJECT, APTITUDE, GENERAL
    score = Column(Integer)
    total_questions = Column(Integer)
    answers_json = Column(JSON) # User's answers map {q_id: selected_index}
    time_taken_seconds = Column(Integer)
    completed_at = Column(DateTime, default=datetime.utcnow)
    
    user = relationship("User")

class StudentFee(Base):
    __tablename__ = "student_fees"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"), unique=True)
    tuition_fee_due = Column(Float, default=85000.0)
    hostel_fee_due = Column(Float, default=30000.0)
    exam_fee_due = Column(Float, default=2500.0)
    library_fine_due = Column(Float, default=150.0)
    updated_at = Column(DateTime, default=datetime.utcnow)
    
    student = relationship("Student")

class FeePaymentTransaction(Base):
    __tablename__ = "fee_payment_transactions"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    amount = Column(Float, nullable=False)
    category = Column(String, nullable=False) # Tuition, Hostel, Exam, Library
    transaction_id = Column(String, unique=True, nullable=False)
    payment_method = Column(String, default="UPI") # UPI, CARD, NET_BANKING
    status = Column(String, default="SUCCESS")
    paid_at = Column(DateTime, default=datetime.utcnow)
    
    student = relationship("Student")

class LibrarySeat(Base):
    __tablename__ = "library_seats"
    
    id = Column(Integer, primary_key=True, index=True)
    seat_no = Column(String, nullable=False)
    floor = Column(Integer, default=1)
    zone = Column(String, default="Quiet Zone") # Quiet Zone, Computer Lab
    is_booked = Column(Boolean, default=False)
    booked_by_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    booking_time_slot = Column(String, nullable=True)
    updated_at = Column(DateTime, default=datetime.utcnow)
    
    booked_by = relationship("User")

class ResumeProfile(Base):
    __tablename__ = "resume_profiles"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"), unique=True)
    full_name = Column(String, nullable=True)
    email = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    linkedin = Column(String, nullable=True)
    github = Column(String, nullable=True)
    education = Column(String, nullable=True)
    experience_json = Column(JSON, default=list)
    projects_json = Column(JSON, default=list)
    skills_json = Column(JSON, default=list)
    updated_at = Column(DateTime, default=datetime.utcnow)
    
    student = relationship("Student")

class ParentPaymentRequest(Base):
    __tablename__ = "parent_payment_requests"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    category = Column(String, nullable=False)
    amount = Column(Float, nullable=False)
    parent_name = Column(String, nullable=False)
    parent_phone = Column(String, nullable=False)
    status = Column(String, default="PENDING")
    created_at = Column(DateTime, default=datetime.utcnow)
    
    student = relationship("Student")
