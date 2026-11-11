from pydantic import BaseModel
from typing import Optional, List, Any
from datetime import date, datetime

# --- Core Schemas ---
class UserBase(BaseModel):
    username: str
    full_name: Optional[str] = None
    email: Optional[str] = None
    role: str

class UserCreate(UserBase):
    password: str

class NotificationBase(BaseModel):
    title: str
    message: str
    type: str

class NotificationOut(NotificationBase):
    id: int
    is_read: bool
    created_at: datetime
    metadata_json: Optional[Any] = None
    class Config: from_attributes = True

# --- Attendance Schemas ---
class ClassSessionCreate(BaseModel):
    subject_id: str
    department: str
    year: int
    section: str

class ClassSessionOut(ClassSessionCreate):
    id: int
    start_time: datetime
    status: str
    class Config: from_attributes = True

class AttendanceMark(BaseModel):
    student_id: int
    subject_id: str
    period: str = "1"
    status: str = "Present"

class BulkAttendanceMark(BaseModel):
    student_ids: List[int]
    subject_id: str
    period: str = "1"
    status: str = "Present"

class AttendanceStudentOut(BaseModel):
    id: int
    name: str
    roll_no: str
    department: str
    year: int
    section: str
    profile_image: Optional[str] = None
    guardian_name: Optional[str] = None
    guardian_mobile: Optional[str] = None
    guardian_email: Optional[str] = None
    class Config: from_attributes = True

class SessionAttendanceUpload(BaseModel):
    session_id: int
    student_ids: List[int] # List of present students

# --- Analytics Schemas ---
class AttendanceSummary(BaseModel):
    total_classes: int
    present: int
    absent: int
    guardian_name: Optional[str] = None
    guardian_mobile: Optional[str] = None
    guardian_email: Optional[str] = None
    percentage: float
    subject_id: Optional[str] = None

class DefaulterEntry(BaseModel):
    student_id: int
    name: str
    roll_no: str
    percentage: float
    total_classes: int
    absent: int

# --- Marks Schemas ---
class MarksCreate(BaseModel):
    student_id: Optional[int] = None
    roll_no: Optional[str] = None
    subject_id: str
    assessment_type: str
    score: float
    total_marks: float

class MarksOut(MarksCreate):
    id: int
    subject_name: Optional[str] = None
    student_name: Optional[str] = None
    roll_no: Optional[str] = None
    class Config: from_attributes = True

class QuickMarkCreate(BaseModel):
    student_id: int
    score: float

class MarksBulkCreate(BaseModel):
    subject_id: str
    assessment_type: str
    total_marks: float
    marks: List[QuickMarkCreate]

# --- Correction Request ---
class CorrectionRequestCreate(BaseModel):
    student_id: int
    subject_id: str
    date: date
    requested_status: str
    reason: str

# --- Legacy Support (Keep simplified versions if needed) ---
class StudentBase(BaseModel):
    name: str
    roll_no: str
    program: Optional[str] = None
    department: str
    guardian_name: Optional[str] = None
    guardian_email: Optional[str] = None
    guardian_mobile: Optional[str] = None
    email: Optional[str] = None
    mobile: Optional[str] = None
    profile_image: Optional[str] = None
    blood_group: Optional[str] = None
class StudentCreate(StudentBase):
    pass

class StudentUpdate(BaseModel):
    name: Optional[str] = None
    roll_no: Optional[str] = None
    program: Optional[str] = None
    department: Optional[str] = None
    year: Optional[int] = None
    section: Optional[str] = None
    guardian_name: Optional[str] = None
    guardian_email: Optional[str] = None
    guardian_mobile: Optional[str] = None
    blood_group: Optional[str] = None

class StudentOut(StudentBase):
    id: int
    class Config: from_attributes = True

# --- Teacher Schemas ---
class TeacherCreate(BaseModel):
    username: str
    password: str
    full_name: str
    email: Optional[str] = None
    mobile: Optional[str] = None
    address: Optional[str] = None
    qualification: Optional[str] = None

class TeacherUpdate(BaseModel):
    username: str
    full_name: str
    password: Optional[str] = None
    email: Optional[str] = None
    mobile: Optional[str] = None
    address: Optional[str] = None
    qualification: Optional[str] = None

class TeacherOut(BaseModel):
    id: int
    username: str
    full_name: Optional[str]
    role: str
    email: Optional[str] = None
    mobile: Optional[str] = None
    address: Optional[str] = None
    qualification: Optional[str] = None
    is_hod: Optional[int] = 0
    hod_department: Optional[str] = None
    profile_image: Optional[str] = None

    class Config:
        from_attributes = True

# --- Subject Schemas ---
class SubjectCreate(BaseModel):
    id: str
    name: str
    teacher_id: Optional[int] = None
    credits: int = 3

class SubjectUpdate(BaseModel):
    name: str
    teacher_id: Optional[int] = None
    credits: Optional[int] = None

class SubjectOut(BaseModel):
    id: str
    name: str
    teacher_id: Optional[int]
    department: Optional[str] = None
    year: Optional[int] = None
    semester: Optional[int] = None
    section: Optional[str] = None
    credits: int = 3
    class Config: from_attributes = True

# --- Notification Schemas ---
class FCMTokenUpdate(BaseModel):
    fcm_token: str

class NoticeCreate(BaseModel):
    title: str
    message: str
    target_audience: str = "ALL" # ALL, STUDENTS, TEACHERS
    # Optional Targeting
    department: Optional[str] = None
    year: Optional[int] = None
    section: Optional[str] = None
    
    image_url: Optional[str] = None
    priority: str = "LOW" # HIGH, MEDIUM, LOW
    expiry_date: Optional[datetime] = None
    
    # Scheduling
    scheduled_at: Optional[datetime] = None
    is_template: bool = False
    template_name: Optional[str] = None

class NoticeOut(NoticeCreate):
    id: int
    sender_id: int
    created_at: datetime
    view_count: int = 0
    is_read: bool = False 
    is_read: bool = False 
    class Config: from_attributes = True

class NoticeReaderOut(BaseModel):
    user_id: int
    name: str
    role: str
    read_at: datetime
    
    class Config:
        from_attributes = True

# --- Chat Assistant ---
class ChatRequest(BaseModel):
    message: str

class ChatResponse(BaseModel):
    reply: str
    action: Optional[str] = None # e.g., "NAVIGATE_ATTENDANCE"

# --- Insights ---
class TrendPoint(BaseModel):
    date: date
    percentage: float

class StudentRisk(BaseModel):
    risk_level: str # HIGH, MEDIUM, LOW
    message: str
    missable_classes: int
    current_percentage: float

class RequiredClasses(BaseModel):
    target_percentage: float
    required_classes: int

# --- Exam Schedule ---
class ExamScheduleCreate(BaseModel):
    subject_id: str
    exam_type: str
    date: date
    start_time: Any 
    end_time: Any
    room: str
    department: str
    year: int
    semester: Optional[int] = None
    section: Optional[str] = None

class ExamScheduleOut(ExamScheduleCreate):
    id: int
    subject_name: Optional[str] = None
    class Config: from_attributes = True

# --- GPA ---
class GPASummary(BaseModel):
    student_id: int
    student_name: str
    total_credits: float
    total_points: float
    cgpa: float
    semester_gpa: Optional[float] = None

class ProctorMeetingCreate(BaseModel):
    student_id: int
    date: date
    remarks: str
    action_taken: Optional[str] = None

class ProctorMeetingOut(ProctorMeetingCreate):
    id: int
    teacher_id: int
    student_name: Optional[str] = None
    student_response: Optional[str] = None
    class Config: from_attributes = True

class SectionMessageCreate(BaseModel):
    message: str
    student_id: Optional[int] = None
    department: Optional[str] = None
    year: Optional[int] = None
    section: Optional[str] = None

class SentMessageOut(BaseModel):
    id: int
    content: str
    target_group: Optional[str] = None
    created_at: datetime
    read_count: int = 0
    total_count: int = 0
    class Config: from_attributes = True

# --- Real-time Chat Schemas ---
class ChatMessageBase(BaseModel):
    group_id: str
    content: Optional[str] = None
    msg_type: str = "TEXT"
    attachment_url: Optional[str] = None
    filename: Optional[str] = None
    file_size: Optional[str] = None
    reply_to_id: Optional[int] = None

class ChatMessageCreate(ChatMessageBase):
    pass

class ChatMessageOut(ChatMessageBase):
    id: int
    sender_id: int
    sender_name: Optional[str] = None
    sender_profile_image: Optional[str] = None
    timestamp: datetime
    is_deleted: bool
    status: Optional[str] = "DELIVERED"
    
    class Config:
        from_attributes = True

class ChatGroupOut(BaseModel):
    group_id: str
    name: str 
    department: Optional[str] = None
    year: Optional[int] = None
    section: Optional[str] = None
    last_message: Optional[ChatMessageOut] = None
    unread_count: int = 0
    profile_image: Optional[str] = None
    
class ChatGroupMetadataUpdate(BaseModel):
    name: Optional[str] = None

class CalendarEventCreate(BaseModel):
    title: str
    date_str: str
    event_type: str

class CalendarEventOut(CalendarEventCreate):
    id: int
    created_at: datetime
    class Config: from_attributes = True

# --- Entertainment Schemas ---

class MovieEventCreate(BaseModel):
    title: str
    description: Optional[str] = None
    poster_url: Optional[str] = None
    trailer_url: Optional[str] = None
    show_time: datetime
    venue: str
    duration_mins: Optional[int] = 120
    total_seats: Optional[int] = 100
    ticket_price: Optional[float] = 0.0

class MovieEventOut(MovieEventCreate):
    id: int
    available_seats: Optional[int] = 100
    status: Optional[str] = "UPCOMING"
    created_at: Optional[datetime] = None
    class Config: from_attributes = True

class MovieBookingCreate(BaseModel):
    movie_id: int
    seats_booked: Optional[int] = 1

class MovieBookingOut(MovieBookingCreate):
    id: int
    student_id: int
    booking_time: Optional[datetime] = None
    status: Optional[str] = "CONFIRMED"
    class Config: from_attributes = True

class SportsTournamentCreate(BaseModel):
    name: str
    sport_type: str
    start_date: date
    end_date: date
    registration_deadline: Optional[date] = None
    banner_url: Optional[str] = None

class SportsTournamentOut(SportsTournamentCreate):
    id: int
    status: Optional[str] = "UPCOMING"
    created_at: Optional[datetime] = None
    class Config: from_attributes = True

class SportsMatchCreate(BaseModel):
    tournament_id: int
    team_a: str
    team_b: str
    match_time: datetime
    venue: str

class SportsMatchOut(SportsMatchCreate):
    id: int
    status: Optional[str] = "SCHEDULED"
    score_team_a: Optional[str] = None
    score_team_b: Optional[str] = None
    winner: Optional[str] = None
    is_live: Optional[bool] = False
    class Config: from_attributes = True
