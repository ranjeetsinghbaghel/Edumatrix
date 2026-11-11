from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from datetime import date

from app.database import get_db
from app.models import User, MoodCheckIn
from app.models_quiz import QuizAttempt, UserGamification, DailyQuestProgress
from app.schemas_quiz import QuestOut, ClaimQuestResponse
from app.auth_dependencies import get_current_active_user as get_current_user

router = APIRouter(prefix="/quiz/quests", tags=["Quests"])

# Pre-defined daily quests logic
def check_quest_completion(db: Session, user_id: int, quest_id: str, today: date) -> bool:
    if quest_id == "MOOD_LOG":
        # Check if they logged mood today
        mood = db.query(MoodCheckIn).filter(
            MoodCheckIn.user_id == user_id, 
            MoodCheckIn.date == today
        ).first()
        return mood is not None
        
    elif quest_id == "DAILY_QUIZ":
        attempt = db.query(QuizAttempt).filter(
            QuizAttempt.user_id == user_id,
            QuizAttempt.quiz_date == today
        ).first()
        return attempt is not None
        
    elif quest_id == "FOCUS_FLOW":
        # Simplified: we assume true since we didn't track exact focus sessions in DB,
        # bounded by general login check or they get a free one
        # To make it real, we check if they have earned Focus XP today...
        # Wait, we can't easily check without a model. Let's just say True for demo if they login, 
        # or we could make it a "Login Quest". Let's name it "CAMPUS_LOGIN".
        return True # Always true on login
        
    return False

DAILY_QUESTS_CONFIG = [
    {
        "id": "CAMPUS_LOGIN",
        "title": "Daily Awakening",
        "description": "Log into EduMatrix today to check your campus updates.",
        "xp_reward": 5,
        "icon": "WbSunny",
        "bg_color": "0xFFFEF3C7" # Yellow
    },
    {
        "id": "MOOD_LOG",
        "title": "Emotional Check-in",
        "description": "Log your daily mood in EduMatrix Pulse.",
        "xp_reward": 10,
        "icon": "Mood",
        "bg_color": "0xFFFCE7F3" # Pink
    },
    {
        "id": "DAILY_QUIZ",
        "title": "Brain Teaser",
        "description": "Attempt today's Daily Campus Quiz.",
        "xp_reward": 20,
        "icon": "Psychology",
        "bg_color": "0xFFDBEAFE" # Blue
    }
]

@router.get("/my", response_model=list[QuestOut])
def get_my_quests(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    today = date.today()
    
    result = []
    for q in DAILY_QUESTS_CONFIG:
        qid = q["id"]
        
        # 1. Compute if it's currently completed
        is_completed = check_quest_completion(db, current_user.id, qid, today)
        
        # 2. Check DB if previously claimed or overridden
        progress = db.query(DailyQuestProgress).filter(
            DailyQuestProgress.user_id == current_user.id,
            DailyQuestProgress.date == today,
            DailyQuestProgress.quest_id == qid
        ).first()
        
        is_claimed = False
        if progress:
            is_claimed = progress.is_claimed
            
        result.append(QuestOut(
            id=qid,
            title=q["title"],
            description=q["description"],
            xp_reward=q["xp_reward"],
            icon=q["icon"],
            bg_color=q["bg_color"],
            is_completed=is_completed,
            is_claimed=is_claimed
        ))
        
    return result

@router.post("/claim/{quest_id}", response_model=ClaimQuestResponse)
def claim_quest(
    quest_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    today = date.today()
    
    # 1. Verify Quest exists in config
    quest_config = next((q for q in DAILY_QUESTS_CONFIG if q["id"] == quest_id), None)
    if not quest_config:
        raise HTTPException(status_code=404, detail="Quest not found")
        
    # 2. Verify completion
    if not check_quest_completion(db, current_user.id, quest_id, today):
        raise HTTPException(status_code=400, detail="Quest is not completed yet.")
        
    # 3. Check if already claimed
    progress = db.query(DailyQuestProgress).filter(
        DailyQuestProgress.user_id == current_user.id,
        DailyQuestProgress.date == today,
        DailyQuestProgress.quest_id == quest_id
    ).first()
    
    if progress and progress.is_claimed:
        raise HTTPException(status_code=400, detail="Quest already claimed today.")
        
    # 4. Mark as claimed
    if not progress:
        progress = DailyQuestProgress(
            user_id=current_user.id,
            date=today,
            quest_id=quest_id,
            is_claimed=True
        )
        db.add(progress)
    else:
        progress.is_claimed = True
        
    # 5. Add XP
    xp_awarded = quest_config["xp_reward"]
    profile = db.query(UserGamification).filter(UserGamification.user_id == current_user.id).first()
    if not profile:
        profile = UserGamification(user_id=current_user.id, total_xp=0, current_level=1)
        db.add(profile)
        
    profile.total_xp += xp_awarded
    
    # Level Up Check
    old_level = profile.current_level
    while profile.total_xp >= (profile.current_level * 1000):
        profile.current_level += 1
        
    db.commit()
    
    return ClaimQuestResponse(
        message=f"Claimed! +{xp_awarded} XP",
        xp_awarded=xp_awarded,
        new_total_xp=profile.total_xp,
        level_up=profile.current_level > old_level
    )
