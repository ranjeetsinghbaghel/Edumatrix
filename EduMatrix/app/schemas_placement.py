from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

# --- Base Schemas ---

class PlacementProfileBase(BaseModel):
    linkedin_link: Optional[str] = None
    github_link: Optional[str] = None
    portfolio_link: Optional[str] = None
    resume_url: Optional[str] = None
    is_actively_looking: bool = True

class PlacementProfileCreate(PlacementProfileBase):
    pass

class PlacementProfileOut(PlacementProfileBase):
    id: int
    student_id: int
    resume_version: int
    readiness_score: float
    updated_at: datetime
    class Config: from_attributes = True

# --- Skill ---
class SkillCreate(BaseModel):
    skill_name: str
    proficiency: str
    document_url: Optional[str] = None
    project_status: str = "ONGOING"

class SkillOut(SkillCreate):
    id: int
    is_verified: bool
    created_at: datetime
    class Config: from_attributes = True

# --- Certification ---
class CertificationCreate(BaseModel):
    name: str
    issuing_org: str
    issue_date: Optional[datetime] = None
    credential_url: Optional[str] = None
    expiry_date: Optional[datetime] = None

class CertificationOut(CertificationCreate):
    id: int
    verification_status: str
    class Config: from_attributes = True

# --- Internship ---
class InternshipCreate(BaseModel):
    company_name: str
    role: str
    start_date: str = '2024-01-01'
    end_date: Optional[str] = None
    is_current: bool = False
    description: Optional[str] = None
    certificate_url: Optional[str] = None
    document_url: Optional[str] = None

class InternshipOut(InternshipCreate):
    id: int
    verification_status: str
    start_date: datetime
    end_date: Optional[datetime] = None
    class Config: from_attributes = True

# --- Project ---
class ProjectCreate(BaseModel):
    title: str
    description: str
    tech_stack: str
    project_url: Optional[str] = None
    github_url: Optional[str] = None
    document_url: Optional[str] = None
    project_status: str = "ONGOING"

class ProjectOut(ProjectCreate):
    id: int
    approval_status: str
    rejection_reason: Optional[str] = None
    submitted_at: datetime
    class Config: from_attributes = True

# --- Event ---
class EventCreate(BaseModel):
    event_name: str
    event_type: str
    date: str = '2024-01-01'
    achievement: Optional[str] = None
    certificate_url: Optional[str] = None
    document_url: Optional[str] = None

class EventOut(EventCreate):
    id: int
    verification_status: str
    date: datetime
    class Config: from_attributes = True
    
# --- Aggregated View ---
class StudentReadinessOut(BaseModel):
    profile: Optional[PlacementProfileOut] = None
    skills: List[SkillOut] = []
    certifications: List[CertificationOut] = []
    internships: List[InternshipOut] = []
    projects: List[ProjectOut] = []
    events: List[EventOut] = []
    
    total_score: float = 0.0
    missing_elements: List[str] = []
