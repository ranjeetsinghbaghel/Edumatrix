package com.auraface.auraface_app.presentation.admin

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.domain.model.*
import com.auraface.auraface_app.data.repository.AdminRepository
import com.auraface.auraface_app.data.repository.AuthRepository
import com.auraface.auraface_app.data.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val authRepo: AuthRepository,
    private val teacherRepo: TeacherRepository
) : ViewModel() {

    var students by mutableStateOf<List<Student>>(emptyList())
        private set

    var attendance by mutableStateOf<List<Attendance>>(emptyList())
        private set

    var dashboardAnalytics by mutableStateOf<com.auraface.auraface_app.data.network.model.DashboardAnalytics?>(null)
        private set

    var defaulterCount by mutableStateOf<com.auraface.auraface_app.data.network.model.DefaulterCount?>(null)
        private set

    var mostAbsentSubjects by mutableStateOf<List<com.auraface.auraface_app.data.network.model.SubjectAbsence>>(emptyList())
        private set

    var reportData by mutableStateOf<com.auraface.auraface_app.data.network.model.AdminReportData?>(null)
        private set

    var profile by mutableStateOf<com.auraface.auraface_app.data.remote.dto.UserProfile?>(null)
        private set

    init {
        loadStudents()
        loadAttendance()
        loadAnalytics()
        loadReports()
        loadProfile()
    }

    private fun loadStudents() {
        viewModelScope.launch {
            students = repository.getStudents()
        }
    }

    private fun loadAttendance() {
        viewModelScope.launch {
            attendance = repository.getAttendance()
        }
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    dashboardAnalytics = repository.getDashboardAnalytics()
                    defaulterCount = repository.getDefaulterCount()
                    mostAbsentSubjects = repository.getMostAbsentSubjects()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(10000) // Poll every 10 seconds for real-time updates
            }
        }
    }

    private fun loadReports() {
        viewModelScope.launch {
            reportData = repository.getDetailedReports()
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            try { profile = authRepo.getProfile() } catch (e: Exception) {}
        }
    }

    fun changePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                teacherRepo.changePassword(newPassword)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update password")
            }
        }
    }

    fun sendNotice(
        title: String,
        message: String,
        target: String,
        priority: String,
        department: String? = null,
        year: String? = null, // Receive as String from UI
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val deptToSend = if (department == "All Depts") null else department
                val yearToSend = if (year == "All Years") null else year?.toIntOrNull()
                
                repository.sendNotice(
                    com.auraface.auraface_app.data.remote.dto.NoticeCreateDto(
                        title = title,
                        message = message,
                        target_audience = target,
                        priority = priority,
                        department = deptToSend,
                        year = yearToSend
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to send notice")
            }
        }
    }

    fun notifyHodsAboutDefaulters(studentIds: List<Int>, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repository.notifyDefaulters(studentIds)
                val msg = res["message"]?.toString() ?: "Notifications sent successfully."
                onSuccess(msg)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to send notifications")
            }
        }
    }

    suspend fun getAcademicCalendar(): List<com.auraface.auraface_app.data.network.api.CalendarEventDTO> {
        return repository.getAcademicCalendar()
    }

    suspend fun addCalendarEvent(title: String, dateStr: String, type: String) {
        repository.addCalendarEvent(title, dateStr, type)
    }

    suspend fun deleteCalendarEvent(id: Int) {
        repository.deleteCalendarEvent(id)
    }
}
