from pydantic import BaseModel
from typing import List, Optional, Dict
from datetime import date, datetime

class QuestionBase(BaseModel):
    category: str
    subcategory: Optional[str] = None
    difficulty: str
    question_text: str
    attachment_url: Optional[str] = None
    options: List[str]
    # explanation removed from base to avoid leaking

class QuestionCreate(QuestionBase):
    correct_option_index: int
    explanation: Optional[str] = None

# New Schema for Single Question
class QuestionCreateRequest(QuestionCreate):
    pass

class QuestionResponse(QuestionBase):
    id: int
    
    class Config:
        from_attributes = True

class QuizAttemptSubmit(BaseModel):
    answers: Dict[int, int] # {question_id: selected_index}
    time_taken_seconds: int

class QuizResultResponse(BaseModel):
    score: int
    total_questions: int
    xp_earned: int
    new_total_xp: int
    new_level: int
    streak_bonus: int
    next_level_xp: int
    progress: float
    correct_answers: Dict[int, int] # {question_id: correct_index}
    explanations: Dict[int, str] # {question_id: explanation}
    actual_marks: Optional[float] = None
    percentage: Optional[float] = None
    grade: Optional[str] = None
    marks_recorded: Optional[bool] = True
    
class LeaderboardEntry(BaseModel):
    rank: int
    username: str
    full_name: str
    total_xp: int
    badges: List[str]

class GamificationProfile(BaseModel):
    user_id: int
    total_xp: int
    current_level: int
    current_streak: int
    badges: List[str]
    title: str # Beginner, Intermediate, etc.
    next_level_xp: int
    progress: float # 0.0 to 1.0 for progress bar
    
    class Config:
        from_attributes = True

class RewardItemOut(BaseModel):
    id: int
    title: str
    description: str
    xp_cost: int
    icon_name: str
    bg_color: str
    stock: int
    is_active: bool
    
    class Config:
        from_attributes = True

class RedeemRequest(BaseModel):
    reward_id: int

class RedeemedRewardOut(BaseModel):
    id: int
    reward_id: int
    redeemed_at: datetime
    status: str
    reward: Optional[RewardItemOut] = None
    
    class Config:
        from_attributes = True

class FocusSessionSubmit(BaseModel):
    duration_minutes: int
    subject_tag: Optional[str] = "General Study"

class DuelWagerCreate(BaseModel):
    target_user_id: int
    wager_amount: int
    category: str = "MIXED"

class QuestOut(BaseModel):
    id: str
    title: str
    description: str
    xp_reward: int
    icon: str
    bg_color: str
    is_completed: bool
    is_claimed: bool

class ClaimQuestResponse(BaseModel):
    message: str
    xp_awarded: int
    new_total_xp: int
    level_up: bool
