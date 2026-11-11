from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.orm import Session
from app.database import get_db
from app import models, models_placement, schemas_placement
from app.auth_dependencies import get_current_active_user
from datetime import datetime
import shutil
import os
import urllib.request
import urllib.error
import json

router = APIRouter(prefix="/placement", tags=["Smart Placement Tracker"])

def get_student_id(user_id: int, db: Session):
    student = db.query(models.Student).filter(models.Student.user_id == user_id).first()
    if not student:
        raise HTTPException(status_code=400, detail="User is not a student")
    return student.id

@router.post("/upload")
async def upload_document(
    file: UploadFile = File(...),
    user: models.User = Depends(get_current_active_user)
):
    safe_filename = f"{datetime.now().timestamp()}_{file.filename}"
    file_location = f"images/placement/{safe_filename}"
    os.makedirs(os.path.dirname(file_location), exist_ok=True)
    
    with open(file_location, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    return {"url": f"/images/placement/{safe_filename}", "filename": file.filename}

# --- Profile ---
@router.get("/profile", response_model=schemas_placement.StudentReadinessOut)
def get_readiness_profile(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = get_student_id(user.id, db)
    
    profile = db.query(models_placement.PlacementProfile).filter_by(student_id=student_id).first()
    skills = db.query(models_placement.PlacementSkill).filter_by(student_id=student_id).all()
    certs = db.query(models_placement.PlacementCertification).filter_by(student_id=student_id).all()
    inter = db.query(models_placement.PlacementInternship).filter_by(student_id=student_id).all()
    projs = db.query(models_placement.PlacementProject).filter_by(student_id=student_id).all()
    events = db.query(models_placement.PlacementEvent).filter_by(student_id=student_id).all()
    
    # Calculate Score
    score = 0.0
    missing = []
    
    if profile and profile.resume_url: score += 20
    else: missing.append("Resume Upload")
    
    if len(skills) >= 3: score += 15
    elif len(skills) > 0: score += 5
    else: missing.append("Add at least 3 skills")
    
    if len(inter) >= 1: score += 25
    else: missing.append("Internship Experience")
    
    if len(projs) >= 2: score += 20
    else: missing.append("Minimum 2 Projects")
    
    if len(certs) >= 1: score += 10
    else: missing.append("Certification")
    
    if len(events) >= 1: score += 10
    else: missing.append("Event Participation")
    
    if score > 100: score = 100
    
    return {
        "profile": profile,
        "skills": skills,
        "certifications": certs,
        "internships": inter,
        "projects": projs,
        "events": events,
        "total_score": score,
        "missing_elements": missing
    }

@router.post("/profile", response_model=schemas_placement.PlacementProfileOut)
def update_profile(
    data: schemas_placement.PlacementProfileCreate,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    student_id = get_student_id(user.id, db)
    profile = db.query(models_placement.PlacementProfile).filter_by(student_id=student_id).first()
    
    if not profile:
        profile = models_placement.PlacementProfile(student_id=student_id)
        db.add(profile)
    
    if data.linkedin_link: profile.linkedin_link = data.linkedin_link
    if data.github_link: profile.github_link = data.github_link
    if data.portfolio_link: profile.portfolio_link = data.portfolio_link
    if data.resume_url: 
        profile.resume_url = data.resume_url
        profile.resume_version += 1
        
    profile.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(profile)
    return profile

# --- Skills ---
@router.post("/skill", response_model=schemas_placement.SkillOut)
def add_skill(skill: schemas_placement.SkillCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    new_skill = models_placement.PlacementSkill(student_id=st_id, **skill.dict())
    db.add(new_skill)
    db.commit()
    db.refresh(new_skill)
    return new_skill

@router.put("/skill/{item_id}", response_model=schemas_placement.SkillOut)
def edit_skill(item_id: int, item: schemas_placement.SkillCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementSkill).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Skill not found or unauthorized")
    for k, v in item.dict().items(): setattr(obj, k, v)
    db.commit()
    db.refresh(obj)
    return obj

@router.delete("/skill/{item_id}")
def delete_skill(item_id: int, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementSkill).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Skill not found or unauthorized")
    db.delete(obj)
    db.commit()
    return {"message": "Skill deleted successfully"}

# --- Certifications ---
@router.post("/certification", response_model=schemas_placement.CertificationOut)
def add_certification(cert: schemas_placement.CertificationCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    new_cert = models_placement.PlacementCertification(student_id=st_id, **cert.dict())
    db.add(new_cert)
    db.commit()
    db.refresh(new_cert)
    return new_cert

@router.put("/certification/{item_id}", response_model=schemas_placement.CertificationOut)
def edit_certification(item_id: int, item: schemas_placement.CertificationCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementCertification).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Certification not found or unauthorized")
    for k, v in item.dict().items(): setattr(obj, k, v)
    db.commit()
    db.refresh(obj)
    return obj

@router.delete("/certification/{item_id}")
def delete_certification(item_id: int, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementCertification).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Certification not found or unauthorized")
    db.delete(obj)
    db.commit()
    return {"message": "Certification deleted successfully"}

# --- Internships ---
@router.post("/internship", response_model=schemas_placement.InternshipOut)
def add_internship(intern: schemas_placement.InternshipCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    
    intern_dict = intern.dict()
    def _parse(d):
        if not d: return None
        try: return datetime.strptime(str(d)[:10], "%Y-%m-%d")
        except: return datetime.utcnow()
        
    intern_dict['start_date'] = _parse(intern_dict.get('start_date'))
    intern_dict['end_date'] = _parse(intern_dict.get('end_date')) if intern_dict.get('end_date') else None

    new_int = models_placement.PlacementInternship(student_id=st_id, **intern_dict)
    db.add(new_int)
    db.commit()
    db.refresh(new_int)
    return new_int

@router.put("/internship/{item_id}", response_model=schemas_placement.InternshipOut)
def edit_internship(item_id: int, item: schemas_placement.InternshipCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementInternship).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Internship not found or unauthorized")
    
    intern_dict = item.dict()
    def _parse(d):
        if not d: return None
        try: return datetime.strptime(str(d)[:10], "%Y-%m-%d")
        except: return datetime.utcnow()
        
    intern_dict['start_date'] = _parse(intern_dict.get('start_date'))
    intern_dict['end_date'] = _parse(intern_dict.get('end_date')) if intern_dict.get('end_date') else None

    for k, v in intern_dict.items(): setattr(obj, k, v)
    db.commit()
    db.refresh(obj)
    return obj

@router.delete("/internship/{item_id}")
def delete_internship(item_id: int, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementInternship).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Internship not found or unauthorized")
    db.delete(obj)
    db.commit()
    return {"message": "Internship deleted successfully"}

# --- Projects ---
@router.post("/project", response_model=schemas_placement.ProjectOut)
def add_project(proj: schemas_placement.ProjectCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    new_proj = models_placement.PlacementProject(student_id=st_id, **proj.dict(), approval_status="PENDING", submitted_at=datetime.utcnow())
    db.add(new_proj)
    db.commit()
    db.refresh(new_proj)
    return new_proj

@router.put("/project/{item_id}", response_model=schemas_placement.ProjectOut)
def edit_project(item_id: int, item: schemas_placement.ProjectCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementProject).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Project not found or unauthorized")
    for k, v in item.dict().items(): setattr(obj, k, v)
    db.commit()
    db.refresh(obj)
    return obj

@router.delete("/project/{item_id}")
def delete_project(item_id: int, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementProject).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Project not found or unauthorized")
    db.delete(obj)
    db.commit()
    return {"message": "Project deleted successfully"}

# --- Events ---
@router.post("/event", response_model=schemas_placement.EventOut)
def add_event(event: schemas_placement.EventCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    
    evt_dict = event.dict()
    def _parse(d):
        if not d: return None
        try: return datetime.strptime(str(d)[:10], "%Y-%m-%d")
        except: return datetime.utcnow()
        
    evt_dict['date'] = _parse(evt_dict.get('date'))

    new_evt = models_placement.PlacementEvent(student_id=st_id, **evt_dict)
    db.add(new_evt)
    db.commit()
    db.refresh(new_evt)
    return new_evt

@router.put("/event/{item_id}", response_model=schemas_placement.EventOut)
def edit_event(item_id: int, item: schemas_placement.EventCreate, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementEvent).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Event not found or unauthorized")
    
    evt_dict = item.dict()
    def _parse(d):
        if not d: return None
        try: return datetime.strptime(str(d)[:10], "%Y-%m-%d")
        except: return datetime.utcnow()
        
    evt_dict['date'] = _parse(evt_dict.get('date'))

    for k, v in evt_dict.items(): setattr(obj, k, v)
    db.commit()
    db.refresh(obj)
    return obj

@router.delete("/event/{item_id}")
def delete_event(item_id: int, user: models.User = Depends(get_current_active_user), db: Session = Depends(get_db)):
    st_id = get_student_id(user.id, db)
    obj = db.query(models_placement.PlacementEvent).filter_by(id=item_id, student_id=st_id).first()
    if not obj: raise HTTPException(404, "Event not found or unauthorized")
    db.delete(obj)
    db.commit()
    return {"message": "Event deleted successfully"}

# --- AI Mock Interview & Resume Builder Additions ---
from app.models_advanced import ResumeProfile
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import json

class ResumeProfileDto(BaseModel):
    full_name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    linkedin: Optional[str] = None
    github: Optional[str] = None
    education: Optional[str] = None
    experience: List[Dict[str, Any]] = []
    projects: List[Dict[str, Any]] = []
    skills: List[str] = []

class StartInterviewRequest(BaseModel):
    role: str

class SubmitInterviewRequest(BaseModel):
    role: str
    answers: List[str]

class AIInterviewResultOut(BaseModel):
    score: int
    overall_feedback: str
    feedback_by_question: List[str]
    strengths: List[str]
    weaknesses: List[str]

INTERVIEW_QUESTIONS = {
    "Android Developer": [
        "What is the difference between launch and async in Kotlin Coroutines, and when would you use each?",
        "Explain the lifecycle of a ViewModel and why it is useful for configuration changes.",
        "How does Jetpack Compose handle state, and what is recomposition?",
        "What is Hilt and how does dependency injection benefit Android applications?",
        "How do you handle background operations in Android using WorkManager?"
    ],
    "Web Developer": [
        "What is the difference between Server-Side Rendering (SSR) and Client-Side Rendering (CSR)?",
        "Explain the concept of RESTful APIs and contrast it with GraphQL.",
        "What is middleware in Express/FastAPI, and how is it useful?",
        "How do you secure web applications against Cross-Site Scripting (XSS) and CSRF attacks?",
        "Explain the differences between SQL and NoSQL databases, and when to use which."
    ],
    "Data Scientist & AI Engineer": [
        "What is the difference between supervised and unsupervised learning? Give examples of both.",
        "Explain what overfitting is and how you can prevent it.",
        "How do transformers and self-attention mechanisms differ from traditional RNNs/LSTMs?",
        "What metrics would you use to evaluate a classification model's performance?",
        "Explain the difference between bagging and boosting algorithms."
    ],
    "Software Engineer (Core)": [
        "Explain the four main principles of Object-Oriented Programming (OOP) with examples.",
        "What is the difference between a process and a thread, and how do they share memory?",
        "What is the time and space complexity of QuickSort, and in what cases does it degrade?",
        "Explain what a deadlock is and how it can be avoided in concurrent programming.",
        "What are design patterns, and can you explain the Singleton or Factory pattern?"
    ],
    "Product Manager": [
        "How do you prioritize features for a product roadmap under tight deadlines and limited resources?",
        "What metrics would you define to measure the success of a newly launched student feed feature?",
        "How do you gather and incorporate user feedback into the product design process?",
        "Explain a time when a product launch failed or faced critical bugs, and how you would handle it.",
        "How do you balance engineering constraints with business requirements and user needs?"
    ]
}

KEYWORD_MATCHERS = {
    "Android Developer": [
        ["async", "await", "launch", "coroutine", "job", "deferred", "thread"],
        ["viewmodel", "lifecycle", "config", "rotation", "save", "state", "retained"],
        ["compose", "state", "recomposition", "remember", "mutable", "ui", "composable"],
        ["inject", "hilt", "dependency", "decouple", "test", "provides", "binds"],
        ["workmanager", "background", "persistent", "service", "job", "periodic", "one-time"]
    ],
    "Web Developer": [
        ["ssr", "csr", "server", "client", "render", "seo", "load", "browser"],
        ["rest", "graphql", "query", "endpoint", "api", "post", "get", "resolver"],
        ["middleware", "request", "response", "intercept", "next", "auth", "log"],
        ["xss", "csrf", "sanitize", "token", "header", "cors", "secure", "cookie"],
        ["sql", "nosql", "relational", "document", "schema", "table", "key", "join"]
    ],
    "Data Scientist & AI Engineer": [
        ["supervised", "unsupervised", "label", "cluster", "train", "regression", "kmeans"],
        ["overfit", "train", "regularize", "dropout", "val", "early stop", "generalize"],
        ["transformer", "attention", "sequence", "rnn", "lstm", "parallel", "weight"],
        ["accuracy", "precision", "recall", "f1", "confusion", "roc", "auc", "metric"],
        ["bagging", "boosting", "random forest", "gradient boost", "weak", "ensemble", "tree"]
    ],
    "Software Engineer (Core)": [
        ["encapsulate", "inherit", "polymorph", "abstract", "oop", "class", "interface"],
        ["process", "thread", "memory", "share", "cpu", "address space", "heap"],
        ["n log n", "log n", "complexity", "worst", "n^2", "pivot", "quick", "sort"],
        ["deadlock", "mutex", "lock", "thread", "wait", "acquire", "prevention"],
        ["singleton", "factory", "pattern", "design", "instance", "creation", "solid"]
    ],
    "Product Manager": [
        ["prioritize", "roadmap", "rice", "impact", "effort", "stakeholder", "deadline"],
        ["metric", "kpi", "mau", "dau", "retention", "engagement", "funnel", "analytics"],
        ["feedback", "interview", "survey", "persona", "user", "iteration", "test"],
        ["fail", "postmortem", "bug", "rollback", "communicate", "fix", "retrospective"],
        ["engineering", "business", "design", "user", "tradeoff", "constraint", "balance"]
    ]
}

def evaluate_locally(role: str, answers: List[str]) -> AIInterviewResultOut:
    questions = INTERVIEW_QUESTIONS.get(role, [])
    matchers = KEYWORD_MATCHERS.get(role, [])
    
    feedbacks = []
    scores = []
    
    for i, ans in enumerate(answers):
        ans_clean = ans.lower().strip()
        if not ans_clean or len(ans_clean) < 10:
            feedbacks.append("Your response was too brief or empty. Please explain with more technical depth and examples.")
            scores.append(20)
            continue
            
        # Match keywords
        role_matchers = matchers[i] if i < len(matchers) else []
        matches = [word for word in role_matchers if word in ans_clean]
        
        score_val = 40 # Base score for writing something
        # Add points for detail length
        score_val += min(30, int(len(ans_clean) / 10))
        # Add points for matches
        score_val += min(30, len(matches) * 10)
        
        scores.append(score_val)
        
        if score_val >= 80:
            feedbacks.append(f"Excellent response! You correctly identified key concepts (including: {', '.join(matches)}) and explained them with solid technical terminology.")
        elif score_val >= 60:
            feedbacks.append(f"Good response, but could be improved. You mentioned relevant concepts like {', '.join(matches)}, but adding concrete implementation details would make it stronger.")
        else:
            feedbacks.append("Your answer touched upon the topic but lacked key architectural components. Review standard definitions and code patterns for this area.")
            
    avg_score = int(sum(scores) / len(scores)) if scores else 0
    
    strengths = []
    weaknesses = []
    
    if avg_score >= 80:
        strengths = ["Strong conceptual understanding of core architectural patterns.", "Good articulation of technical tradeoffs and framework components."]
        weaknesses = ["Could provide more deep dive examples of edge-case scenarios."]
    elif avg_score >= 60:
        strengths = ["Basic knowledge of framework components is solid.", "Good response length and communication effort."]
        weaknesses = ["Needs to detail configuration details and performance bottlenecks.", "Incorporate more industry-standard terminology in explanations."]
    else:
        strengths = ["Willingness to attempt all technical prompts."]
        weaknesses = ["Needs a comprehensive review of fundamental framework structures.", "Ensure you understand lifecycle implications and asynchronous flows."]
        
    return AIInterviewResultOut(
        score=avg_score,
        overall_feedback=f"Overall, you performed at an {'Advanced' if avg_score >= 80 else 'Intermediate' if avg_score >= 60 else 'Beginner'} level for an {role} role. Your technical foundation is solid but could benefit from focusing on detail-oriented implementation patterns.",
        feedback_by_question=feedbacks,
        strengths=strengths,
        weaknesses=weaknesses
    )

# --- Resume Routes ---
@router.get("/resume", response_model=ResumeProfileDto)
def get_resume_profile(
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    st_id = get_student_id(user.id, db)
    res_prof = db.query(ResumeProfile).filter(ResumeProfile.student_id == st_id).first()
    
    # If not found, fetch existing placement profile fields for ease
    student = db.query(models.Student).filter(models.Student.id == st_id).first()
    p_profile = db.query(models_placement.PlacementProfile).filter(models_placement.PlacementProfile.student_id == st_id).first()
    p_skills = db.query(models_placement.PlacementSkill).filter(models_placement.PlacementSkill.student_id == st_id).all()
    p_projects = db.query(models_placement.PlacementProject).filter(models_placement.PlacementProject.student_id == st_id).all()
    
    if not res_prof:
        # Prepopulate
        res_prof = ResumeProfile(
            student_id=st_id,
            full_name=student.name if student else user.full_name,
            email=student.email if student else user.email,
            phone=student.mobile if student else "",
            linkedin=p_profile.linkedin_link if p_profile else "",
            github=p_profile.github_link if p_profile else "",
            education=f"GIET University, B.Tech in {student.department if student else 'Engineering'}, Year {student.year if student else 1}",
            skills_json=[s.skill_name for s in p_skills],
            projects_json=[{"title": p.title, "tech_stack": p.tech_stack, "description": p.description} for p in p_projects],
            experience_json=[]
        )
        db.add(res_prof)
        db.commit()
        db.refresh(res_prof)
        
    return ResumeProfileDto(
        full_name=res_prof.full_name,
        email=res_prof.email,
        phone=res_prof.phone,
        linkedin=res_prof.linkedin,
        github=res_prof.github,
        education=res_prof.education,
        experience=res_prof.experience_json or [],
        projects=res_prof.projects_json or [],
        skills=res_prof.skills_json or []
    )

@router.post("/resume/save")
def save_resume_profile(
    req: ResumeProfileDto,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    st_id = get_student_id(user.id, db)
    res_prof = db.query(ResumeProfile).filter(ResumeProfile.student_id == st_id).first()
    if not res_prof:
        res_prof = ResumeProfile(student_id=st_id)
        db.add(res_prof)
        
    res_prof.full_name = req.full_name
    res_prof.email = req.email
    res_prof.phone = req.phone
    res_prof.linkedin = req.linkedin
    res_prof.github = req.github
    res_prof.education = req.education
    res_prof.experience_json = req.experience
    res_prof.projects_json = req.projects
    res_prof.skills_json = req.skills
    res_prof.updated_at = datetime.utcnow()
    
    db.commit()
    return {"message": "Resume profile updated successfully"}

# --- Interview Routes ---
@router.post("/interview/start", response_model=List[str])
def start_interview(
    req: StartInterviewRequest,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    role = req.role.strip()
    if role not in INTERVIEW_QUESTIONS:
        raise HTTPException(status_code=400, detail=f"Interview questions not available for role: {role}")
    return INTERVIEW_QUESTIONS[role]

@router.post("/interview/submit", response_model=AIInterviewResultOut)
def submit_interview(
    req: SubmitInterviewRequest,
    user: models.User = Depends(get_current_active_user),
    db: Session = Depends(get_db)
):
    role = req.role.strip()
    if role not in INTERVIEW_QUESTIONS:
        raise HTTPException(status_code=400, detail="Invalid role")
    if len(req.answers) != 5:
        raise HTTPException(status_code=400, detail="Must submit exactly 5 answers")
        
    # Attempt to use Google Gemini API
    gemini_key = os.getenv("GEMINI_API_KEY")
    if gemini_key:
        try:
            prompt = f"""
            You are an elite interviewer for a {role} position.
            Evaluate the candidate's answers to the following 5 questions:
            
            Question 1: {INTERVIEW_QUESTIONS[role][0]}
            Answer 1: {req.answers[0]}
            
            Question 2: {INTERVIEW_QUESTIONS[role][1]}
            Answer 2: {req.answers[1]}
            
            Question 3: {INTERVIEW_QUESTIONS[role][2]}
            Answer 3: {req.answers[2]}
            
            Question 4: {INTERVIEW_QUESTIONS[role][3]}
            Answer 4: {req.answers[3]}
            
            Question 5: {INTERVIEW_QUESTIONS[role][4]}
            Answer 5: {req.answers[4]}
            
            Provide your assessment in strict JSON format matching this Pydantic schema:
            {{
                "score": <int between 0 and 100>,
                "overall_feedback": "<text summary of performance>",
                "feedback_by_question": ["feedback for q1", "feedback for q2", "feedback for q3", "feedback for q4", "feedback for q5"],
                "strengths": ["strength1", "strength2"],
                "weaknesses": ["weakness1", "weakness2"]
            }}
            Do not add any markdown formatting (like ```json or ```) or text outside the JSON. Return only the raw JSON.
            """
            
            url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={gemini_key}"
            headers = {"Content-Type": "application/json"}
            payload = {
                "contents": [{
                    "parts": [{"text": prompt}]
                }]
            }
            
            request = urllib.request.Request(
                url, 
                data=json.dumps(payload).encode("utf-8"), 
                headers=headers, 
                method="POST"
            )
            
            with urllib.request.urlopen(request, timeout=15) as response:
                res_data = json.loads(response.read().decode("utf-8"))
                res_text = res_data["candidates"][0]["content"]["parts"][0]["text"].strip()
                
            # Clean response if markdown ticks are returned
            if res_text.startswith("```json"):
                res_text = res_text[7:]
            elif res_text.startswith("```"):
                res_text = res_text[3:]
            if res_text.endswith("```"):
                res_text = res_text[:-3]
            res_text = res_text.strip()
            
            data = json.loads(res_text)
            return AIInterviewResultOut(
                score=int(data["score"]),
                overall_feedback=data["overall_feedback"],
                feedback_by_question=data["feedback_by_question"],
                strengths=data["strengths"],
                weaknesses=data["weaknesses"]
            )
        except Exception as e:
            print(f"Gemini API Interview failed: {e}. Falling back to local keyword evaluation.")
            return evaluate_locally(role, req.answers)
    else:
        print("GEMINI_API_KEY not set. Using local keyword evaluation.")
        return evaluate_locally(role, req.answers)

