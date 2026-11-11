# EduMatrix 🎓
### Next-Generation Smart Campus ERP, Biometric Attendance & Academic Management System

**EduMatrix** is a comprehensive, production-grade university management and campus intelligence ecosystem designed for modern higher-education institutions. Tailored specifically for **GIET University (GIETU), Gunupur**, it seamlessly integrates high-speed AI face recognition attendance, dynamic academic timetable scheduling, realistic daily quizzes with official gradebook synchronization, personalized student study planning, and full campus utility services.

---

## 🌟 Key Features

### 1. 📅 Dynamic Timetable & Academic Scheduling
- **Section & Room Mapping**: Pre-configured for **VII Semester B.Tech Computer Science & Engineering (Section-G)** in Room **AME-16**.
- **Period Structure**:
  - Period I: 10:00 AM – 11:00 AM
  - Period II: 11:00 AM – 12:00 PM
  - Period III: 12:00 PM – 01:00 PM
- **Full Faculty & Subject Legend**:
  - **CNS**: Computer Networks & Security (*Ms. Supriya Satapathy*)
  - **CD**: Compiler Design (*Dr. D. Anil Kumar*)
  - **DA**: Data Analytics (*Dr. Bhabani Sankar Panda*)
  - **CG**: Computer Graphics (*Mr. Kedarnath Panda*)
  - **EED / ED**: Entrepreneurship & Enterprise Development (*Dr. A. Chiranjibi Rambabu Achary*)
  - **SEMINAR**: Technical Seminar (*Mrs. A. Jyoshana*)
  - **PROJECT**: Major Project Phase-I (*Dr. D. Anil Kumar*)

### 2. 📝 Realistic Daily Quiz & Official Gradebook Integration
- **50+ Real Syllabus-Aligned Questions**: High-quality multiple-choice questions covering core 7th-semester engineering disciplines.
- **Automated Academic Grading**: Evaluates student submissions, calculates percentage, assigns letter grades (`A+` to `F`), and records the marks directly into the university's permanent `marks` table under `assessment_type = "Daily Quiz"`.
- **Instant Result Feedback**: Shows immediate score breakdowns and explanations for every question.

### 3. 🧠 AI-Driven Personalized Study Planner
- **Student-Centric Planning**: Dynamically analyzes each student's current grades, quiz performance, weak areas, and daily timetable.
- **Priority-Weighted Directives**: Assigns **HIGH**, **MEDIUM**, and **NORMAL** study priorities to keep students ahead in coursework and exams.
- **Interactive Tracking**: Real-time progress bar and completion checklists in the Android app.

### 4. 📸 Smart Face Recognition & Biometric Attendance
- Anti-spoofing facial recognition pipeline utilizing state-of-the-art embeddings.
- Automatic verification against enrolled student face vectors with real-time class attendance logging.

### 5. 🏫 Comprehensive Campus Ecosystem
- **GIET ERP Portal**: Fee payments, hostel management, hall tickets, and results lookup.
- **Campus Amenities**: Canteen pre-ordering, Sports ground booking, Spaces reservation, Healthcare center appointment booking, and Campus Lost & Found.
- **Placement & Internships**: Live campus drive updates, resume readiness tracking, and internship application workflows.

---

## 🏗️ System Architecture

```
EduMatrix/
├── EduMatrix/               # FastAPI Backend Service (Python 3.10+)
│   ├── app/
│   │   ├── main.py          # Application entrypoint & route registration
│   │   ├── database.py      # SQLAlchemy DB session engine
│   │   ├── models.py        # Core database models (User, Attendance, Marks, Schedule)
│   │   ├── models_quiz.py   # Quiz questions, attempts, and evaluation schemas
│   │   ├── routes/          # RESTful modular API routers
│   │   └── static/          # Web assets, brand assets & logo
│   └── requirements.txt     # Python dependencies
│
├── EduMatrix_App/           # Native Android Mobile Application
│   ├── app/src/main/java/   # Jetpack Compose UI, MVVM, Retrofit & Coroutines
│   └── build.gradle.kts     # Android build configuration (SDK 34)
│
└── Output/                  # Compiled artifacts & documentation
```

---

## 🚀 Getting Started

### Backend Setup (FastAPI)
1. **Navigate to the backend directory:**
   ```bash
   cd EduMatrix
   ```
2. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```
3. **Run the server:**
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
   ```
4. **Access Swagger API Docs:** Open `http://localhost:8000/docs` in your browser.

### Android Application Setup
1. Open the `EduMatrix_App` folder in **Android Studio** (Hedgehog or Ladybug recommended).
2. Sync the project with Gradle files.
3. Configure your backend server base URL in `network_security_config.xml` or API constants.
4. Build and install:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 👥 Faculty & Coordination
- **Department Timetable Coordinator:** Mrs. A. Jyoshana
- **Institution:** Gandhi Institute of Engineering and Technology University (GIETU), Gunupur, Odisha
- **Department:** Computer Science & Engineering (School of Engineering and Technology)
