package com.auraface.auraface_app.presentation.teacher

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.repository.AuthRepository
import com.auraface.auraface_app.data.repository.TeacherRepository
import com.auraface.auraface_app.data.network.model.TimetableItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val repo: TeacherRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    var profile by mutableStateOf<com.auraface.auraface_app.data.remote.dto.UserProfile?>(null)
        private set

    private val _teachers = kotlinx.coroutines.flow.MutableStateFlow<List<com.auraface.auraface_app.domain.model.Teacher>>(emptyList())
    val teachers = _teachers.asStateFlow()

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    var timetable by mutableStateOf<List<TimetableItem>>(emptyList())
        private set

    var dashboardData by mutableStateOf<com.auraface.auraface_app.data.network.model.TeacherAnalytics?>(null)
        private set

    var subjectHistory by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set

    var sectionLeaveRequests by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set

    var hodLeaveRequests by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
        
    var subjectMarks by mutableStateOf<List<com.auraface.auraface_app.data.network.model.MarksResponse>>(emptyList())
        private set

    init {
        loadDashboardData()
        loadProfile()
        loadTimetable()
    }

    fun loadTimetable() {
        viewModelScope.launch {
            try { 
                timetable = repo.getTimetable()
                android.util.Log.d("TeacherViewModel", "Timetable loaded: ${timetable.size} items")
                timetable.forEach { item ->
                    android.util.Log.d("TeacherViewModel", "Timetable item: ${item.subject}, Teacher: ${item.teacher}")
                }
            } catch(e: Exception) {
                android.util.Log.e("TeacherViewModel", "Error loading timetable: ${e.message}", e)
            }
        }
    }

    fun loadTeachers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _teachers.value = repo.getTeachers()
            } catch (e: Exception) {}
            _isLoading.value = false
        }
    }

    fun addTeacher(teacher: com.auraface.auraface_app.domain.model.Teacher) {
        viewModelScope.launch {
            try {
                repo.addTeacher(teacher, null)
                loadTeachers()
            } catch (e: Exception) {
                android.util.Log.e("TeacherViewModel", "Error adding teacher: ${e.message}", e)
            }
        }
    }

    fun updateTeacher(teacher: com.auraface.auraface_app.domain.model.Teacher) {
        viewModelScope.launch {
            try {
                repo.updateTeacher(teacher, null)
                loadTeachers()
            } catch (e: Exception) {
                android.util.Log.e("TeacherViewModel", "Error updating teacher: ${e.message}", e)
            }
        }
    }

    fun deleteTeacher(id: Int) {
        viewModelScope.launch {
            try {
                repo.deleteTeacher(id)
                loadTeachers()
            } catch (e: Exception) {
                android.util.Log.e("TeacherViewModel", "Error deleting teacher: ${e.message}", e)
            }
        }
    }


    fun assignHod(teacherId: Int, department: String) {
        viewModelScope.launch {
            repo.assignHod(teacherId, department)
        }
    }

    fun assignClassTeacher(teacherId: Int, department: String, year: Int, semester: Int, section: String) {
        viewModelScope.launch {
            repo.assignClassTeacher(teacherId, department, year, semester, section)
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            try { 
                profile = authRepo.getProfile()
                android.util.Log.d("TeacherViewModel", "Profile loaded: name=${profile?.name}, username=${profile?.username}")
            } catch(e: Exception) {
                android.util.Log.e("TeacherViewModel", "Error loading profile: ${e.message}", e)
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    dashboardData = repo.getTeacherDashboardData()
                    android.util.Log.d("TeacherViewModel", "Dashboard data loaded: ${dashboardData?.assignedSubjects?.size} subjects")
                    if (dashboardData?.isClassTeacher == true) loadSectionLeaves()
                    if (dashboardData?.isHod == true) loadHodLeaves()
                } catch (e: Exception) {
                    android.util.Log.e("TeacherViewModel", "Error loading dashboard data: ${e.message}", e)
                }
                delay(10000) // Poll every 10s
            }
        }
    }

    fun updateAvailability(message: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repo.updateAvailability(message)
                val oldData = dashboardData
                if (oldData != null) {
                    dashboardData = oldData.copy(customAvailabilityMessage = message)
                } else {
                    loadDashboardData()
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update availability")
            }
        }
    }

    fun updateAvailabilityToggle(isAvailable: Boolean, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.updateAvailabilityToggle(isAvailable)
                val oldData = dashboardData
                if (oldData != null) {
                    dashboardData = oldData.copy(isAvailable = isAvailable)
                } else {
                    loadDashboardData()
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to toggle availability")
            }
        }
    }

    fun loadSectionLeaves() {
        viewModelScope.launch {
            try { sectionLeaveRequests = repo.getSectionLeaves() } catch(e: Exception) {
                android.util.Log.e("TeacherViewModel", "Error loading section leaves: ${e.message}")
            }
        }
    }

    fun loadHodLeaves() {
        viewModelScope.launch {
            try { hodLeaveRequests = repo.getHodLeaves() } catch(e: Exception) {}
        }
    }

    fun approveSectionLeave(leaveId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.sectionAction(leaveId, "approve")
                loadSectionLeaves()
                onSuccess()
            } catch(e: Exception) {
                e.printStackTrace()
                android.util.Log.e("TeacherViewModel", "Error approving section leave: ${e.message}")
            }
        }
    }

    fun rejectSectionLeave(leaveId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.sectionAction(leaveId, "reject")
                loadSectionLeaves()
                onSuccess()
            } catch(e: Exception) {
                 e.printStackTrace()
                 android.util.Log.e("TeacherViewModel", "Error rejected section leave: ${e.message}")
            }
        }
    }

    fun hodAction(leaveId: Int, approve: Boolean, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.hodAction(leaveId, approve)
                loadHodLeaves()
                onSuccess()
            } catch(e: Exception) {
                e.printStackTrace()
                android.util.Log.e("TeacherViewModel", "Error in hodAction: ${e.message}")
            }
        }
    }

    fun loadSubjectHistory(subjectId: String) {
        viewModelScope.launch {
            try {
                subjectHistory = repo.getSubjectHistory(subjectId)
            } catch (e: Exception) {
                subjectHistory = emptyList()
            }
        }
    }

    fun changePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repo.changePassword(newPassword)
                onSuccess()
            } catch(e: Exception) {
                onError(e.message ?: "Failed to update password")
            }
        }
    }

    fun requestMakeupClass(
        date: String,
        timeSlot: String,
        subjectId: String,
        department: String,
        year: Int,
        semester: Int,
        section: String,
        room: String?,
        reason: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.createMakeupClass(
                    com.auraface.auraface_app.data.network.model.MakeupClassCreate(
                        date, timeSlot, subjectId, department, year, semester, section, room, reason
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to schedule class")
            }
        }
    }
    fun addMarks(
        studentId: Int?,
        rollNo: String?,
        subjectId: String,
        assessmentType: String,
        score: Float,
        totalMarks: Float,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repo.addMarks(
                    com.auraface.auraface_app.data.network.model.MarksCreateRequest(
                        student_id = studentId,
                        roll_no = rollNo,
                        subject_id = subjectId,
                        assessment_type = assessmentType,
                        score = score,
                        total_marks = totalMarks
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to upload marks")
            }
        }
    }

    fun loadSubjectMarks(subjectId: String) {
        viewModelScope.launch {
            try {
                subjectMarks = repo.getSubjectMarks(subjectId)
            } catch (e: Exception) {
                subjectMarks = emptyList()
            }
        }
    }

    var studentsForMarking by mutableStateOf<List<com.auraface.auraface_app.data.network.model.StudentBasic>>(emptyList())
        private set

    fun getStudentsForClass(department: String, year: Int, section: String) {
        viewModelScope.launch {
            try {
                studentsForMarking = repo.getStudentsForClass(department, year, section)
            } catch (e: Exception) {
                studentsForMarking = emptyList()
            }
        }
    }

    fun addMarksBulk(
        subjectId: String,
        assessmentType: String,
        totalMarks: Float,
        marks: List<com.auraface.auraface_app.data.network.model.QuickMarkCreate>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val res = repo.addMarksBulk(
                    com.auraface.auraface_app.data.network.model.MarksBulkCreate(
                        subject_id = subjectId,
                        assessment_type = assessmentType,
                        total_marks = totalMarks,
                        marks = marks
                    )
                )
                if (res.success == true) onSuccess() else onError(res.error?.message ?: "Failed")
            } catch (e: Exception) {
                onError(e.message ?: "Failed")
            }
        }
    }

    var subjectsForSelection by mutableStateOf<List<com.auraface.auraface_app.data.network.model.SubjectDTO>>(emptyList())
        private set

    fun getSubjects(department: String?, year: Int?, section: String?) {
        viewModelScope.launch {
            try {
                subjectsForSelection = repo.getSubjects(department, year, section)
            } catch (e: Exception) {
                subjectsForSelection = emptyList()
            }
        }
    }

    var proctorMeetings by mutableStateOf<List<com.auraface.auraface_app.data.network.model.ProctorMeeting>>(emptyList())
        private set

    fun getProctorMeetings(studentId: Int? = null) {
        viewModelScope.launch {
            try {
                proctorMeetings = repo.getProctorMeetings(studentId)
            } catch (e: Exception) {
                proctorMeetings = emptyList()
            }
        }
    }

    fun addProctorMeeting(studentId: Int, date: String, remarks: String, action: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.addProctorMeeting(
                    com.auraface.auraface_app.data.network.model.ProctorMeetingCreate(studentId, date, remarks, action)
                )
                onSuccess()
                getProctorMeetings() // refresh
            } catch (e: Exception) {
                // handle error
                // handle error
            }
        }
    }

    var myClassStudents by mutableStateOf<List<com.auraface.auraface_app.data.network.model.StudentBasic>>(emptyList())
        private set

    fun getMyClassStudents(department: String, year: Int, section: String) {
        viewModelScope.launch {
            try {
                myClassStudents = repo.getStudentsForClass(department, year, section)
            } catch (e: Exception) {
                myClassStudents = emptyList()
            }
        }
    }

    fun getStudentResults(studentId: Int, onResult: (List<com.auraface.auraface_app.data.network.model.MarksResponse>) -> Unit) {
        viewModelScope.launch {
            try {
                val results = repo.getStudentResults(studentId)
                onResult(results)
            } catch (e: Exception) {
                onResult(emptyList())
            }
        }
    }

    fun sendSectionMessage(message: String, studentId: Int? = null, department: String? = null, year: Int? = null, section: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repo.sendSectionMessage(message, studentId, department, year, section)
                onSuccess()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var notifications by mutableStateOf<List<com.auraface.auraface_app.data.network.model.Notification>>(emptyList())
        private set

    fun loadNotifications() {
        viewModelScope.launch {
            try { notifications = repo.getNotifications() } catch(e: Exception) {}
        }
    }

    var sentMessages by mutableStateOf<List<com.auraface.auraface_app.data.network.model.SentMessage>>(emptyList())
        private set

    fun loadSentMessages() {
        viewModelScope.launch {
            try { sentMessages = repo.getSentMessages() } catch(e: Exception) {}
        }
    }

    // Student Directory & Search
    var searchStudentsList by mutableStateOf<List<com.auraface.auraface_app.data.network.api.TeacherStudentSearchItem>>(emptyList())
        private set
    var isSearchingStudents by mutableStateOf(false)
        private set
    var selectedStudentDetail by mutableStateOf<com.auraface.auraface_app.data.network.api.TeacherStudentProfileDetail?>(null)
        private set
    var isLoadingStudentDetail by mutableStateOf(false)
        private set

    fun searchStudents(
        year: Int? = null,
        department: String? = null,
        semester: Int? = null,
        section: String? = null,
        query: String? = null
    ) {
        viewModelScope.launch {
            isSearchingStudents = true
            searchStudentsList = repo.searchStudents(
                year = year,
                department = department,
                semester = semester,
                section = section,
                query = query
            )
            isSearchingStudents = false
        }
    }

    fun loadStudentDetail(studentId: Int) {
        viewModelScope.launch {
            isLoadingStudentDetail = true
            repo.getStudentProfileDetail(studentId)
                .onSuccess {
                    selectedStudentDetail = it
                }
                .onFailure {
                    selectedStudentDetail = null
                }
            isLoadingStudentDetail = false
        }
    }

    fun clearSelectedStudent() {
        selectedStudentDetail = null
    }
}

