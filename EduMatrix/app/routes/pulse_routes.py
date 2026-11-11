from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime, date
from typing import List

from app.database import get_db
from app.models import User, MoodCheckIn, Student
from app.models_quiz import UserGamification
from app.schemas_pulse import MoodCheckInCreate, MoodCheckInOut, PulseDashboardOut, DailyPulseInsight
from app.auth_dependencies import get_current_active_user as get_current_user

router = APIRouter(prefix="/pulse", tags=["Aura Pulse"])

@router.post("/checkin", response_model=MoodCheckInOut)
def record_daily_mood(
    req: MoodCheckInCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    today = datetime.utcnow().date()
    
    # Check if already checked in today
    existing = db.query(MoodCheckIn).filter(
        MoodCheckIn.user_id == current_user.id,
        MoodCheckIn.date == today
    ).first()
    
    if existing:
        # Update existing mood
        existing.mood = req.mood
        existing.notes = req.notes
        existing.timestamp = datetime.utcnow()
        xp_rewarded = 0
    else:
        # Create new check-in
        new_checkin = MoodCheckIn(
            user_id=current_user.id,
            mood=req.mood,
            notes=req.notes,
            date=today
        )
        db.add(new_checkin)
        
        # Reward 10 XP for checking in
        xp_rewarded = 10
        profile = db.query(UserGamification).filter(UserGamification.user_id == current_user.id).first()
        if not profile:
            profile = UserGamification(user_id=current_user.id, total_xp=0, current_level=1)
            db.add(profile)
        
        profile.total_xp += xp_rewarded
        
        # Level up logic
        while profile.total_xp >= (profile.current_level * 1000):
            profile.current_level += 1
            
        existing = new_checkin

    db.commit()
    db.refresh(existing)
    
    # Return as output model with dynamic XP info
    return MoodCheckInOut(
        id=existing.id,
        user_id=existing.user_id,
        date=existing.date,
        mood=existing.mood,
        notes=existing.notes,
        timestamp=existing.timestamp,
        xp_rewarded=xp_rewarded
    )

@router.get("/my-history", response_model=List[MoodCheckInOut])
def get_my_mood_history(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    return db.query(MoodCheckIn).filter(
        MoodCheckIn.user_id == current_user.id
    ).order_by(MoodCheckIn.date.desc()).all()


@router.get("/insights", response_model=PulseDashboardOut)
def get_campus_insights(
    department: str = Query(None, description="Filter by department"),
    year: int = Query(None, description="Filter by year"),
    target_date: date = Query(default_factory=datetime.utcnow().date),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    # Only Admin or Teachers should ideally see this, but we'll allow it generally, maybe restricted later.
    
    query = db.query(MoodCheckIn.mood, func.count(MoodCheckIn.id).label('count'))\
              .join(Student, MoodCheckIn.user_id == Student.user_id)\
              .filter(MoodCheckIn.date == target_date)
              
    if department:
        query = query.filter(Student.department == department)
    if year:
        query = query.filter(Student.year == year)
        
    results = query.group_by(MoodCheckIn.mood).all()
    
    total_students = sum(r[1] for r in results)
    
    insights = []
    dominant_mood = "Unknown"
    max_count = 0
    
    for r in results:
        mood, count = r[0], r[1]
        percentage = (count / total_students * 100) if total_students > 0 else 0
        insights.append(DailyPulseInsight(mood=mood, count=count, percentage=percentage))
        
        if count > max_count:
            max_count = count
            dominant_mood = mood
            
    # Sort insights by count
    insights.sort(key=lambda x: x.count, reverse=True)
    
    return PulseDashboardOut(
        total_students=total_students,
        dominant_mood=dominant_mood,
        insights=insights
    )
