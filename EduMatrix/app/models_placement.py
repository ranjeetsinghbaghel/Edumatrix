from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, DateTime, Float, Enum
from sqlalchemy.orm import relationship
from app.database import Base
from datetime import datetime

class PlacementProfile(Base):
    __tablename__ = "placement_profiles"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"), unique=True)
    
    linkedin_link = Column(String, nullable=True)
    github_link = Column(String, nullable=True)
    portfolio_link = Column(String, nullable=True)
    resume_url = Column(String, nullable=True)
    resume_version = Column(Integer, default=1)
    
    # Readiness Logic
    readiness_score = Column(Float, default=0.0) # 0 to 100
    is_actively_looking = Column(Boolean, default=True)
    
    updated_at = Column(DateTime, default=datetime.utcnow)
    
    student = relationship("Student", backref="placement_profile")

class PlacementSkill(Base):
    __tablename__ = "placement_skills"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    
    skill_name = Column(String)
    proficiency = Column(String) # Beginner, Intermediate, Advanced, Expert
    is_verified = Column(Boolean, default=False)
    document_url = Column(String, nullable=True)
    
    created_at = Column(DateTime, default=datetime.utcnow)

class PlacementCertification(Base):
    __tablename__ = "placement_certifications"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    
    name = Column(String)
    issuing_org = Column(String)
    issue_date = Column(DateTime, nullable=True)
    credential_url = Column(String, nullable=True)
    expiry_date = Column(DateTime, nullable=True)
    
    verification_status = Column(String, default="PENDING") # PENDING, VERIFIED, REJECTED
    
class PlacementInternship(Base):
    __tablename__ = "placement_internships"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    
    company_name = Column(String)
    role = Column(String)
    start_date = Column(DateTime)
    end_date = Column(DateTime, nullable=True)
    is_current = Column(Boolean, default=False)
    description = Column(String, nullable=True)
    certificate_url = Column(String, nullable=True)
    document_url = Column(String, nullable=True)
    
    verification_status = Column(String, default="PENDING")

class PlacementProject(Base):
    __tablename__ = "placement_projects"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    
    title = Column(String)
    description = Column(String)
    tech_stack = Column(String) # Comma separated
    project_url = Column(String, nullable=True)
    github_url = Column(String, nullable=True)
    project_status = Column(String, default="ONGOING")
    document_url = Column(String, nullable=True)
    
    # Approval Workflow
    approval_status = Column(String, default="PENDING") # PENDING, APPROVED, REJECTED
    reviewer_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    rejection_reason = Column(String, nullable=True)
    submitted_at = Column(DateTime, default=datetime.utcnow)
    approved_at = Column(DateTime, nullable=True)

class PlacementEvent(Base):
    __tablename__ = "placement_events"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    
    event_name = Column(String)
    event_type = Column(String) # Hackathon, Workshop, Seminar
    date = Column(DateTime)
    achievement = Column(String, nullable=True) # "First Place", "Participant"
    certificate_url = Column(String, nullable=True)
    document_url = Column(String, nullable=True)
    
    verification_status = Column(String, default="PENDING")
