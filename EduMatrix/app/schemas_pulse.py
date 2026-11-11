from pydantic import BaseModel
from typing import Optional, List
from datetime import date, datetime

class MoodCheckInCreate(BaseModel):
    mood: str
    notes: Optional[str] = None

class MoodCheckInOut(BaseModel):
    id: int
    user_id: int
    date: date
    mood: str
    notes: Optional[str] = None
    timestamp: datetime
    xp_rewarded: Optional[int] = 0

    class Config:
        from_attributes = True

class DailyPulseInsight(BaseModel):
    mood: str
    count: int
    percentage: float

class PulseDashboardOut(BaseModel):
    total_students: int
    dominant_mood: str
    insights: List[DailyPulseInsight]
