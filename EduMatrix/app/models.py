from datetime import datetime, date
from pydantic import BaseModel
from sqlalchemy import Column, Integer, String, Date, Time, Boolean, ForeignKey, UniqueConstraint, Table, JSON, DateTime, Float
from sqlalchemy.orm import relationship
from app.database import Base

# --- RBAC Models ---
role_permissions = Table(
    "role_permissions",
    Base.metadata,
    Column("role_name", String, ForeignKey("roles.name")),
    Column("permission_name", String, ForeignKey("permissions.name"))
)

class Role(Base):
    __tablename__ = "roles"
    name = Column(String, primary_key=True)  # "admin", "teacher", "student"
    permissions = relationship("Permission", secondary=role_permissions, backref="roles")

class Permission(Base):
    __tablename__ = "permissions"
    name = Column(String, primary_key=True)  # e.g., "USER_CREATE"
    description = Column(String, nullable=True)

# --- Core User Model ---
class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True)
    password = Column(String)
    role = Column(String, ForeignKey("roles.name"))  # Linked to Role table
    is_active = Column(Boolean, default=True)

    # Core Profile
    full_name = Column(String, nullable=True)
    email = Column(String, nullable=True)
    mobile = Column(String, nullable=True)
    profile_image = Column(String, nullable=True)
    
    # HOD / Staff Specific
    is_hod = Column(Integer, default=0)
    hod_department = Column(String, nullable=True)
    position = Column(String, nullable=True)
    address = Column(String, nullable=True)
    qualification = Column(String, nullable=True)
    otp = Column(String, nullable=True)
    fcm_token = Column(String, nullable=True)
    custom_availability_message = Column(String, default="8 AM to 7 PM")
    is_available = Column(Boolean, default=True)

class Notice(Base):
    __tablename__ = "notices"
    
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    message = Column(String, nullable=False)
    sender_id = Column(Integer, ForeignKey("users.id"))
    target_audience = Column(String, default="ALL") # ALL, TEACHERS, STUDENTS
    created_at = Column(DateTime, default=datetime.utcnow)
    image_url = Column(String, nullable=True)
    priority = Column(String, default="LOW") # HIGH, MEDIUM, LOW
    expiry_date = Column(DateTime, nullable=True)
    
    # Targeting
    department = Column(String, nullable=True)
    year = Column(Integer, nullable=True)
    section = Column(String, nullable=True)
    
    # Scheduling & Templates
    scheduled_at = Column(DateTime, nullable=True)
    is_template = Column(Boolean, default=False)
    template_name = Column(String, nullable=True)
    
    reads = relationship("NoticeRead", back_populates="notice")

class NoticeRead(Base):
    __tablename__ = "notice_reads"
    id = Column(Integer, primary_key=True, index=True)
    notice_id = Column(Integer, ForeignKey("notices.id"))
    user_id = Column(Integer, ForeignKey("users.id"))
    read_at = Column(DateTime, default=datetime.utcnow)
    
    notice = relationship("Notice", back_populates="reads")
    user = relationship("User")

class Student(Base):
    __tablename__ = "students"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    name = Column(String, nullable=False)
    roll_no = Column(String, unique=True, index=True)
    program = Column(String, nullable=True) # e.g., B.Tech, M.Tech, MCA, BCA
    department = Column(String)
    year = Column(Integer)
    semester = Column(Integer, nullable=True)
    section = Column(String)
    
    user = relationship("User")
    
    @property
    def profile_image(self):
        return self.user.profile_image if self.user else None

    @property
    def email(self):
        return self.user.email if self.user else None

    @property
    def mobile(self):
        return self.user.mobile if self.user else None
    
    # Guardian Info
    guardian_name = Column(String, nullable=True)
    guardian_email = Column(String, nullable=True)
    guardian_mobile = Column(String, nullable=True)
    
    blood_group = Column(String, nullable=True)

# --- Academic Models ---
class Subject(Base):
    __tablename__ = "subjects"
    id = Column(String, primary_key=True)
    name = Column(String)
    teacher_id = Column(Integer, ForeignKey("users.id"))
    department = Column(String, nullable=True)
    year = Column(Integer, nullable=True)
    semester = Column(Integer, nullable=True)
    section = Column(String, nullable=True)
    credits = Column(Integer, default=3)

class ClassSession(Base):
    __tablename__ = "class_sessions"
    
    id = Column(Integer, primary_key=True, index=True)
    teacher_id = Column(Integer, ForeignKey("users.id"))
    subject_id = Column(String, ForeignKey("subjects.id"))
    date = Column(Date, default=date.today)
    start_time = Column(DateTime, default=datetime.now)
    end_time = Column(DateTime, nullable=True)
    status = Column(String, default="ACTIVE") # ACTIVE, CLOSED
    room = Column(String, nullable=True)
    is_makeup = Column(Boolean, default=False)
    
    # For quick lookup
    department = Column(String)
    year = Column(Integer)
    section = Column(String)

class Attendance(Base):
    __tablename__ = "attendance"

    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("class_sessions.id"), nullable=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    subject = Column(String, index=True) # Redundant but useful for fast queries
    date = Column(Date, index=True)
    time = Column(Time, nullable=True)
    status = Column(String, default="Present") # Present, Absent, Late
    period = Column(String, nullable=True) # Legacy support
    
    # New fields for Late Marking Rules
    is_late = Column(Boolean, default=False)
    late_reason = Column(String, nullable=True)
    marked_at = Column(DateTime, default=datetime.utcnow)

    student = relationship("Student")
    
    __table_args__ = (
        UniqueConstraint("student_id", "session_id", name="unique_session_attendance"),
    )

class CorrectionRequest(Base):
    __tablename__ = "correction_requests"
    
    id = Column(Integer, primary_key=True, index=True)
    requester_id = Column(Integer, ForeignKey("users.id")) # Teacher
    student_id = Column(Integer, ForeignKey("students.id"))
    date = Column(Date)
    subject_id = Column(String)
    current_status = Column(String)
    requested_status = Column(String)
    reason = Column(String)
    status = Column(String, default="PENDING") # PENDING, APPROVED, REJECTED
    admin_remark = Column(String, nullable=True)

class Marks(Base):
    __tablename__ = "marks"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    subject_id = Column(String, ForeignKey("subjects.id"))
    assessment_type = Column(String) # Midterm, Final, Assignment
    score = Column(Float)
    total_marks = Column(Float)
    
    __table_args__ = (
        UniqueConstraint("student_id", "subject_id", "assessment_type", name="unique_marks_entry"),
    )
    
    subject = relationship("Subject")

# --- System Models ---
class Notification(Base):
    __tablename__ = "notifications"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    title = Column(String, nullable=False)
    message = Column(String, nullable=False)
    type = Column(String, default="SYSTEM") # ATTENDANCE, MARKS, LEAVE, SYSTEM
    is_read = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    metadata_json = Column(JSON, nullable=True)
    sent_message_id = Column(Integer, ForeignKey("sent_messages.id"), nullable=True)

class SentMessage(Base):
    __tablename__ = "sent_messages"
    
    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, ForeignKey("users.id"))
    content = Column(String)
    target_group = Column(String) # e.g., "CSE 3rd Year A"
    created_at = Column(DateTime, default=datetime.utcnow)
    
    notifications = relationship("Notification") # Backref


class AuditLog(Base):
    __tablename__ = "audit_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    actor_user_id = Column(Integer, ForeignKey("users.id"))
    action = Column(String) # CREATE, UPDATE, DELETE, LOGIN
    entity_type = Column(String) # USER, ATTENDANCE, MARKS
    entity_id = Column(String)
    details = Column(JSON, nullable=True)
    ip_address = Column(String, nullable=True)
    timestamp = Column(DateTime, default=datetime.utcnow)

class LeaveRequest(Base): # Kept for backward compatibility logic
    __tablename__ = "leave_requests"
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("students.id"))
    reason = Column(String)
    date = Column(Date, default=date.today)
    status = Column(String, default="Pending_Class_Teacher") 
    student = relationship("Student")

class ClassSchedule(Base):
    __tablename__ = "class_schedule"

    id = Column(Integer, primary_key=True, index=True)
    department = Column(String)
    year = Column(Integer)
    semester = Column(Integer, nullable=True)
    section = Column(String)
    day_of_week = Column(String) # Monday, Tuesday...
    time_slot = Column(String) # "09:00-10:00"
    subject = Column(String)
    teacher_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    period = Column(String, default="1")
    room = Column(String, nullable=True)
    date = Column(Date, nullable=True) # Optional override for specific date
    status = Column(String, default="APPROVED") # PENDING, APPROVED, REJECTED
    request_reason = Column(String, nullable=True) # For Make-up requests

class ExamSchedule(Base): # Use SQLAlchemy Base
    __tablename__ = "exam_schedule"
    
    id = Column(Integer, primary_key=True, index=True)
    subject_id = Column(String, ForeignKey("subjects.id"))
    exam_type = Column(String) # "Midterm", "Final", "Quiz"
    date = Column(Date)
    start_time = Column(Time)
    end_time = Column(Time)
    room = Column(String)
    
    # Target Group
    department = Column(String)
    year = Column(Integer)
    semester = Column(Integer, nullable=True)
    section = Column(String, nullable=True)
    
    subject = relationship("Subject")

# Update ClassSession to link to Schedule if needed, or stick to standalone
# We will rely on ClassSchedule for the "Builder" and ClassSession for "Daily Instance"

# Update Attendance for Late Marking
# We need to use `extend_existing=True` if we were reloading, but since we are editing source:
# We'll just define the fields.
# Note: I'm appending to the end of file, but I need to make sure I don't break existing classes.
# Actually I need to EDIT existing classes.

class SectionMetadata(Base):
    __tablename__ = "section_metadata"
    
    id = Column(Integer, primary_key=True, index=True)
    department = Column(String)
    year = Column(Integer)
    semester = Column(Integer, nullable=True)
    section = Column(String)
    class_teacher_id = Column(Integer, ForeignKey("users.id"))
    
    __table_args__ = (
        UniqueConstraint("department", "year", "section", name="unique_section_meta"),
    )

class ProctorMeeting(Base):
    __tablename__ = "proctor_meetings"
    
    id = Column(Integer, primary_key=True, index=True)
    teacher_id = Column(Integer, ForeignKey("users.id"))
    student_id = Column(Integer, ForeignKey("students.id"))
    date = Column(Date, default=date.today)
    remarks = Column(String)
    action_taken = Column(String, nullable=True)
    student_response = Column(String, nullable=True) # Student Discussion Notes
    
    student = relationship("Student")
    teacher = relationship("User")


from app.models_gallery import GalleryFolder, GalleryImage
from app.models_placement import PlacementProfile, PlacementSkill, PlacementCertification, PlacementInternship, PlacementProject, PlacementEvent


# --- Chat System Models ---
class ChatGroupMetadata(Base):
    __tablename__ = "chat_group_metadata"
    
    group_id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=True) # Override default name
    profile_image = Column(String, nullable=True)
    created_by = Column(Integer, ForeignKey("users.id"), nullable=True)

class ChatMessage(Base):
    __tablename__ = "chat_messages"

    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, ForeignKey("users.id"))
    group_id = Column(String, index=True) # e.g., "CSE-3-A"
    content = Column(String, nullable=True) 
    timestamp = Column(DateTime, default=datetime.utcnow)
    
    # Message type & Media
    msg_type = Column(String, default="TEXT") # TEXT, IMAGE, PDF, VIDEO
    attachment_url = Column(String, nullable=True)
    filename = Column(String, nullable=True) # Name of the file
    file_size = Column(String, nullable=True) # e.g., "2MB"
    
    # Reply logic
    reply_to_id = Column(Integer, ForeignKey("chat_messages.id"), nullable=True)
    
    # Metadata
    # Storing sender name for faster retrieval in lists, though joining is cleaner.
    # We will join for now.
    
    is_deleted = Column(Boolean, default=False)
    
    # Relationships
    sender = relationship("User")
    parent = relationship("ChatMessage", remote_side=[id])
    reads = relationship("MessageReadStatus", back_populates="message", cascade="all, delete-orphan")

    @property
    def sender_name(self):
        return self.sender.full_name if self.sender and self.sender.full_name else (self.sender.username if self.sender else "Unknown")

    @property
    def sender_profile_image(self):
        return self.sender.profile_image if self.sender else None

class MessageReadStatus(Base):
    __tablename__ = "message_read_status"
    
    id = Column(Integer, primary_key=True, index=True)
    message_id = Column(Integer, ForeignKey("chat_messages.id"))
    user_id = Column(Integer, ForeignKey("users.id"))
    status = Column(String, default="DELIVERED") # DELIVERED, READ
    updated_at = Column(DateTime, default=datetime.utcnow)
    
    message = relationship("ChatMessage", back_populates="reads")

class MoodCheckIn(Base):
    __tablename__ = "mood_checkins"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    date = Column(Date, default=datetime.utcnow().date)
    mood = Column(String) # HAPPY, FOCUSED, NEUTRAL, TIRED, STRESSED
    notes = Column(String, nullable=True)
    timestamp = Column(DateTime, default=datetime.utcnow)
    
    user = relationship("User")

class CalendarEvent(Base):
    __tablename__ = "calendar_events"
    
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    date_str = Column(String, nullable=False) # e.g., "15 Oct - 20 Oct"
    event_type = Column(String, nullable=False) # "Holiday", "Exam", "Deadline"
    created_at = Column(DateTime, default=datetime.utcnow)

# --- Entertainment Models ---

class MovieEvent(Base):
    __tablename__ = "movie_events"
    
    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, nullable=False)
    description = Column(String, nullable=True)
    poster_url = Column(String, nullable=True)
    trailer_url = Column(String, nullable=True)
    show_time = Column(DateTime, nullable=False)
    venue = Column(String, nullable=False)
    duration_mins = Column(Integer, default=120)
    total_seats = Column(Integer, default=100)
    available_seats = Column(Integer, default=100)
    ticket_price = Column(Float, default=0.0) # E.g., XP points or real money
    status = Column(String, default="UPCOMING") # UPCOMING, NOW_SHOWING, COMPLETED
    created_at = Column(DateTime, default=datetime.utcnow)

class MovieBooking(Base):
    __tablename__ = "movie_bookings"
    
    id = Column(Integer, primary_key=True, index=True)
    movie_id = Column(Integer, ForeignKey("movie_events.id"))
    student_id = Column(Integer, ForeignKey("students.id"))
    seats_booked = Column(Integer, default=1)
    booking_time = Column(DateTime, default=datetime.utcnow)
    status = Column(String, default="CONFIRMED") # CONFIRMED, CANCELLED
    
    movie = relationship("MovieEvent")
    student = relationship("Student")

class SportsTournament(Base):
    __tablename__ = "sports_tournaments"
    
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    sport_type = Column(String, nullable=False) # e.g., Cricket, Football, Basketball
    start_date = Column(Date, nullable=False)
    end_date = Column(Date, nullable=False)
    registration_deadline = Column(Date, nullable=True)
    status = Column(String, default="UPCOMING") # UPCOMING, ONGOING, COMPLETED
    banner_url = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

class SportsMatch(Base):
    __tablename__ = "sports_matches"
    
    id = Column(Integer, primary_key=True, index=True)
    tournament_id = Column(Integer, ForeignKey("sports_tournaments.id"))
    team_a = Column(String, nullable=False) # e.g., "CSE 3rd Year"
    team_b = Column(String, nullable=False)
    match_time = Column(DateTime, nullable=False)
    venue = Column(String, nullable=False)
    status = Column(String, default="SCHEDULED") # SCHEDULED, LIVE, COMPLETED
    score_team_a = Column(String, nullable=True) # e.g., "120/4" or "2"
    score_team_b = Column(String, nullable=True)
    winner = Column(String, nullable=True)
    is_live = Column(Boolean, default=False)
    
    tournament = relationship("SportsTournament")
