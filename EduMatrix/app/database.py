import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# Fetch database URL from environment variable (used in cloud like Render/Supabase)
# Fallback to local MySQL if not found (used during local development)
DATABASE_URL = os.getenv("DATABASE_URL", "mysql+pymysql://root:Ranjeet@localhost/project")

# Render uses `postgres://` which is deprecated in SQLAlchemy 1.4+ (Needs `postgresql://`)
if DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

# SQLite needs "check_same_thread": False. Postgres does not.
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}

engine = create_engine(
    DATABASE_URL,
    connect_args=connect_args
)

SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

Base = declarative_base()


# ✅ Dependency for FastAPI
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
