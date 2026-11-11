import os
import sys
from sqlalchemy import create_engine, MetaData, Table, select, insert, text
from sqlalchemy.ext.compiler import compiles
from sqlalchemy.types import String

# Force UTF-8 encoding for stdout
sys.stdout.reconfigure(encoding='utf-8')

# Custom compilation listener to map String (without length) to VARCHAR(255) on MySQL
@compiles(String, "mysql")
def compile_string_mysql(element, compiler, **kw):
    # If length is not specified, use 255. Also append collation if present.
    length = element.length if element.length is not None else 255
    collation_str = f" COLLATE {element.collation}" if getattr(element, 'collation', None) else ""
    return f"VARCHAR({length}){collation_str}"

# Database connection engines
SQLITE_DB_PATH = "D:/7th Sem/Project/EduMatrix/attendance.db"
MYSQL_URL = "mysql+pymysql://root:Ranjeet@localhost/project"

print(f"Connecting to SQLite database: {SQLITE_DB_PATH}")
sqlite_engine = create_engine(f"sqlite:///{SQLITE_DB_PATH}")

print("Connecting to MySQL database...")
mysql_engine = create_engine(MYSQL_URL)

# Import all models to register them with SQLAlchemy Base metadata
sys.path.append(os.path.abspath(os.path.dirname(__file__)))

from app.database import Base
import app.models
import app.models_advanced
import app.models_campus
import app.models_gallery
import app.models_placement
import app.models_quiz

# Force case-sensitive binary collation for username column in MySQL users table
if 'users' in Base.metadata.tables:
    users_table = Base.metadata.tables['users']
    if 'username' in users_table.columns:
        print("Setting collation of users.username to utf8mb4_bin (case-sensitive)...")
        users_table.columns['username'].type.collation = 'utf8mb4_bin'

print("Generating tables in MySQL if they do not exist...")
Base.metadata.create_all(bind=mysql_engine)
print("MySQL tables ready!")

# Reflect metadata from SQLite database
sqlite_meta = MetaData()
sqlite_meta.reflect(bind=sqlite_engine)

print("Starting table-by-table migration...")

with mysql_engine.begin() as mysql_conn:
    # Disable foreign key checks
    print("Disabling MySQL foreign key checks...")
    mysql_conn.execute(text("SET FOREIGN_KEY_CHECKS = 0;"))
    
    # Iterate through all tables reflected from SQLite
    for table_name in sqlite_meta.tables:
        if table_name == 'sqlite_sequence':
            continue
            
        sqlite_table = sqlite_meta.tables[table_name]
        
        # Read all rows from SQLite
        with sqlite_engine.connect() as sqlite_conn:
            rows = sqlite_conn.execute(select(sqlite_table)).fetchall()
            
        if not rows:
            print(f"Table `{table_name}`: 0 rows. Skipping.")
            continue
            
        print(f"Table `{table_name}`: Found {len(rows)} rows to copy.")
        
        # Map rows to list of dictionaries
        keys = sqlite_table.columns.keys()
        insert_data = []
        for row in rows:
            row_dict = {}
            for k, val in zip(keys, row):
                row_dict[k] = val
            insert_data.append(row_dict)
            
        # Get target Table object
        if table_name in Base.metadata.tables:
            mysql_table = Base.metadata.tables[table_name]
        else:
            mysql_table = Table(table_name, Base.metadata, autoload_with=mysql_engine)
            
        # Clean any existing rows
        mysql_conn.execute(mysql_table.delete())
        
        # Bulk insert rows
        mysql_conn.execute(insert(mysql_table), insert_data)
        print(f"Table `{table_name}`: Successfully copied {len(insert_data)} rows.")

    # Re-enable foreign key checks
    print("Re-enabling MySQL foreign key checks...")
    mysql_conn.execute(text("SET FOREIGN_KEY_CHECKS = 1;"))

print("\n=== Database Migration Completed Successfully! ===")
