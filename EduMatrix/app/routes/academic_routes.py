
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import date

from app.database import get_db
from app.models import ExamSchedule, Marks, Subject, Student, User
from app.schemas import ExamScheduleCreate, ExamScheduleOut, MarksCreate, MarksOut, GPASummary, MarksBulkCreate, SubjectOut
from app.auth import get_current_user

router = APIRouter(
    prefix="/academic",
    tags=["Academic"]
)

# --- Subjects ---

@router.get("/subjects", response_model=List[SubjectOut])
def get_subjects(
    department: Optional[str] = None,
    year: Optional[int] = None,
    section: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user)
):
    if current_user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Not authorized")
        
    query = db.query(Subject)
    if department: query = query.filter(Subject.department == department)
    if year: query = query.filter(Subject.year == year)
    if section: query = query.filter(Subject.section == section)
    
    return query.all()

# --- Exam Schedule ---

@router.post("/exams", response_model=ExamScheduleOut)
def create_exam_schedule(exam: ExamScheduleCreate, db: Session = Depends(get_db), current_user: dict = Depends(get_current_user)):
    if current_user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Not authorized")
    
    new_exam = ExamSchedule(**exam.dict())
    db.add(new_exam)
    db.commit()
    db.refresh(new_exam)
    return new_exam

@router.get("/exams", response_model=List[ExamScheduleOut])
def get_exam_schedule(
    department: Optional[str] = None,
    year: Optional[int] = None,
    section: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user)
):
    query = db.query(ExamSchedule)
    
    # If student, strictly filter by their details
    if current_user["role"] == "student":
        student = db.query(Student).filter(Student.user_id == current_user["id"]).first()
        if not student:
            raise HTTPException(status_code=404, detail="Student profile not found")
        query = query.filter(
            ExamSchedule.department == student.department,
            ExamSchedule.year == student.year
        )
        if student.section:
             query = query.filter(ExamSchedule.section == student.section)
             
    # If teacher/admin, allow manual filtering
    else:
        if department:
            query = query.filter(ExamSchedule.department == department)
        if year:
            query = query.filter(ExamSchedule.year == year)
        if section:
            query = query.filter(ExamSchedule.section == section)
            
    exams = query.all()
    # Enrich with subject name
    for exam in exams:
        if exam.subject:
            exam.subject_name = exam.subject.name
            
    return exams

# --- Results / Marks ---

@router.post("/marks", response_model=MarksOut)
def add_marks(mark: MarksCreate, db: Session = Depends(get_db), current_user: dict = Depends(get_current_user)):
    if current_user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Not authorized")
        
    # Resolve Student ID
    student_id = mark.student_id
    if not student_id and mark.roll_no:
        student = db.query(Student).filter(Student.roll_no == mark.roll_no).first()
        if not student:
            raise HTTPException(status_code=404, detail=f"Student with Roll No {mark.roll_no} not found")
        student_id = student.id
    
    if not student_id:
        raise HTTPException(status_code=400, detail="Either student_id or roll_no must be provided")

    # Check if exists
    existing = db.query(Marks).filter(
        Marks.student_id == student_id,
        Marks.subject_id == mark.subject_id,
        Marks.assessment_type == mark.assessment_type
    ).first()
    
    if existing:
        existing.score = mark.score
        existing.total_marks = mark.total_marks
        db.commit()
        db.refresh(existing)
        return existing
    
    new_mark = Marks(
        student_id=student_id,
        subject_id=mark.subject_id,
        assessment_type=mark.assessment_type,
        score=mark.score,
        total_marks=mark.total_marks
    )
    db.add(new_mark)
    db.commit()
    db.refresh(new_mark)
    return new_mark

@router.post("/marks/bulk")
def add_marks_bulk(bulk: MarksBulkCreate, db: Session = Depends(get_db), current_user: dict = Depends(get_current_user)):
    if current_user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Not authorized")
        
    for m in bulk.marks:
        existing = db.query(Marks).filter(
            Marks.student_id == m.student_id,
            Marks.subject_id == bulk.subject_id,
            Marks.assessment_type == bulk.assessment_type
        ).first()
        
        if existing:
            existing.score = m.score
            existing.total_marks = bulk.total_marks
        else:
            new_mark = Marks(
                student_id=m.student_id,
                subject_id=bulk.subject_id,
                assessment_type=bulk.assessment_type,
                score=m.score,
                total_marks=bulk.total_marks
            )
            db.add(new_mark)
            
    db.commit()
    return {"message": "Marks updated successfully"}

@router.get("/results/me", response_model=List[MarksOut])
def get_my_results(db: Session = Depends(get_db), current_user: dict = Depends(get_current_user)):
    if current_user["role"] != "student":
         raise HTTPException(status_code=403, detail="Only students can view their own results here")
         
    student = db.query(Student).filter(Student.user_id == current_user["id"]).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student profile not found")
        
    results = db.query(Marks).filter(Marks.student_id == student.id).all()
    
    for res in results:
        if res.subject:
            res.subject_name = res.subject.name
            
    return results

@router.get("/results/student/{student_id}", response_model=List[MarksOut])
def get_student_results(student_id: int, db: Session = Depends(get_db), current_user: dict = Depends(get_current_user)):
    if current_user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Not authorized")
        
    results = db.query(Marks).filter(Marks.student_id == student_id).all()
    
    for res in results:
        if res.subject:
            res.subject_name = res.subject.name
            
    return results

@router.get("/marks/subject/{subject_id}", response_model=List[MarksOut])
def get_subject_marks(
    subject_id: str,
    db: Session = Depends(get_db),
    current_user: dict = Depends(get_current_user)
):
    if current_user["role"] not in ["admin", "teacher"]:
        raise HTTPException(status_code=403, detail="Not authorized")
        
    marks = db.query(Marks).filter(Marks.subject_id == subject_id).all()
    
    for mark in marks:
        student = db.query(Student).filter(Student.id == mark.student_id).first()
        if student:
            mark.student_name = student.name
            mark.roll_no = student.roll_no
            
    return marks

# --- CGPA Calculation ---

@router.get("/cgpa/me", response_model=GPASummary)
def calculate_my_cgpa(db: Session = Depends(get_db), current_user: dict = Depends(get_current_user)):
    if current_user["role"] != "student":
         raise HTTPException(status_code=403, detail="Only students.")
         
    student = db.query(Student).filter(Student.user_id == current_user["id"]).first()
    if not student:
        raise HTTPException(status_code=404, detail="Student not found")
        
    return _calculate_gpa(student, db)

def _calculate_gpa(student: Student, db: Session) -> GPASummary:
    marks = db.query(Marks).filter(Marks.student_id == student.id).all()
    
    total_points = 0
    total_credits = 0
    
    for mark in marks:
        subject = db.query(Subject).filter(Subject.id == mark.subject_id).first()
        credits = subject.credits if subject and hasattr(subject, 'credits') else 3 # Default 3
        
        # Simple GPA Logic: Percentage -> 10 scale
        percentage = (mark.score / mark.total_marks) * 100
        grade_point = 0
        
        if percentage >= 90: grade_point = 10
        elif percentage >= 80: grade_point = 9
        elif percentage >= 70: grade_point = 8
        elif percentage >= 60: grade_point = 7
        elif percentage >= 50: grade_point = 6
        elif percentage >= 40: grade_point = 5
        else: grade_point = 0
        
        total_points += (grade_point * credits)
        total_credits += credits
        
    cgpa = 0.0
    if total_credits > 0:
        cgpa = total_points / total_credits
        
    return GPASummary(
        student_id=student.id,
        student_name=student.name,
        total_credits=total_credits,
        total_points=total_points,
        cgpa=round(cgpa, 2)
    )
