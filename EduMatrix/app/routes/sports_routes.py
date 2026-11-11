from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import SportsTournament, SportsMatch, Student
from app.auth import get_current_user, admin_or_teacher
from pydantic import BaseModel
from typing import List
from ..schemas import SportsTournamentCreate, SportsTournamentOut, SportsMatchCreate, SportsMatchOut

router = APIRouter(prefix="/sports", tags=["Sports"])

# --- Student Ends ---

@router.get("/tournaments", response_model=List[SportsTournamentOut])
def get_tournaments(db: Session = Depends(get_db)):
    return db.query(SportsTournament).filter(SportsTournament.status.in_(["UPCOMING", "ONGOING"])).all()

@router.get("/tournaments/{tournament_id}/matches", response_model=List[SportsMatchOut])
def get_matches_for_tournament(tournament_id: int, db: Session = Depends(get_db)):
    return db.query(SportsMatch).filter(SportsMatch.tournament_id == tournament_id).all()

@router.get("/live-matches", response_model=List[SportsMatchOut])
def get_live_matches(db: Session = Depends(get_db)):
    return db.query(SportsMatch).filter(SportsMatch.is_live == True).all()

# --- Admin Ends ---

@router.post("/tournaments", response_model=SportsTournamentOut)
def create_tournament(
    req: SportsTournamentCreate,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    tournament = SportsTournament(**req.dict())
    db.add(tournament)
    db.commit()
    db.refresh(tournament)
    return tournament

@router.post("/matches", response_model=SportsMatchOut)
def add_match(
    req: SportsMatchCreate,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    match = SportsMatch(**req.dict())
    db.add(match)
    db.commit()
    db.refresh(match)
    return match

class UpdateScoreReq(BaseModel):
    score_team_a: str
    score_team_b: str
    is_live: bool = True
    status: str = "LIVE"

@router.put("/matches/{match_id}/score", response_model=SportsMatchOut)
def update_score(
    match_id: int,
    req: UpdateScoreReq,
    db: Session = Depends(get_db),
    user=Depends(admin_or_teacher)
):
    match = db.query(SportsMatch).filter(SportsMatch.id == match_id).first()
    if not match:
        raise HTTPException(status_code=404, detail="Match not found")
        
    match.score_team_a = req.score_team_a
    match.score_team_b = req.score_team_b
    match.is_live = req.is_live
    match.status = req.status
    
    db.commit()
    db.refresh(match)
    return match
