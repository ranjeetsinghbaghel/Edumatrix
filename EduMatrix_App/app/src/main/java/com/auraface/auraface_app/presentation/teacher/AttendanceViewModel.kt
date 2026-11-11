package com.auraface.auraface_app.presentation.teacher

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.core.utils.LocationUtils
import com.auraface.auraface_app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _result = MutableLiveData<String?>()
    val result: LiveData<String?> = _result

    fun markAttendance(
        subjectId: String,
        period: String,
        base64Image: String,
        startTime: String? = null,
        endTime: String? = null
    ) {
        viewModelScope.launch {
            try {
                val response = repository.markAttendance(
                    subjectId = subjectId,
                    period = period,
                    imageBase64 = base64Image,
                    startTime = startTime,
                    endTime = endTime
                )

                _result.postValue(response.message)

            } catch (e: Exception) {
                _result.postValue("Attendance failed: ${e.message}")
            }
        }
    }

    fun markAttendanceManual(
        subjectId: String,
        period: String,
        rollNo: String,
        startTime: String? = null,
        endTime: String? = null
    ) {
        viewModelScope.launch {
            try {
                // In a real app, this might call a different endpoint or the same with extra params
                val response = repository.markAttendanceManual(
                    subjectId = subjectId,
                    period = period,
                    rollNo = rollNo,
                    startTime = startTime,
                    endTime = endTime
                )
                _result.postValue(response.message)
            } catch (e: Exception) {
                _result.postValue("Manual entry failed: ${e.message}")
            }
        }
    }

    var studentsList by mutableStateOf<List<com.auraface.auraface_app.data.network.model.AttendanceStudent>>(emptyList())
        private set
    
    private var allClassStudents = listOf<com.auraface.auraface_app.data.network.model.AttendanceStudent>()

    var attendanceStep by mutableStateOf("Marking") // Marking, Review
        private set

    var shareableSummary by mutableStateOf<String?>(null)
        private set
        
    fun clearSummary() {
        shareableSummary = null
    }

    fun getStudentsByClass(year: Int, section: String) {
        viewModelScope.launch {
            try {
                allClassStudents = repository.getStudentsByClass(year, section)
                studentsList = allClassStudents
                attendanceStep = "Marking"
                shareableSummary = null
            } catch (e: Exception) {
                _result.postValue("Failed to load students: ${e.message}")
            }
        }
    }

    fun toggleStudentSelection(studentId: Int) {
        studentsList = studentsList.map { 
            if (it.id == studentId) it.copy(isSelected = !it.isSelected) else it 
        }
    }

    fun markBulkAttendance(subjectId: String, period: String) {
        viewModelScope.launch {
            val selectedIds = studentsList.filter { it.isSelected }.map { it.id }
            
            if (attendanceStep == "Marking") {
                if (selectedIds.isNotEmpty()) {
                    try {
                        val response = repository.markBulkAttendance(selectedIds, subjectId, period)
                        _result.postValue("Marked ${selectedIds.size} students present. Now review absent list.")
                        
                        // Transition to Review Step:
                        // 1. Keep only Unselected (Absent) students
                        // 2. Clear their selection so user can pick from them
                        studentsList = studentsList.filter { !it.isSelected }.map { it.copy(isSelected = false) }
                        attendanceStep = "Review"
                        
                    } catch (e: Exception) {
                        _result.postValue("Submission failed: ${e.message}")
                    }
                } else {
                    // No one present?
                   _result.postValue("No students marked present. Proceeding to absent review.")
                   studentsList = studentsList.map { it.copy(isSelected = false) }
                   attendanceStep = "Review"
                }
            } 
            else if (attendanceStep == "Review") {
                // Now selectedIds are the "Corrections" (Marked Absent -> Present)
                if (selectedIds.isNotEmpty()) {
                    try {
                        val response = repository.markBulkAttendance(selectedIds, subjectId, period)
                        _result.postValue("Updated. ${selectedIds.size} more marked present.")
                    } catch (e: Exception) {
                        _result.postValue("Update failed: ${e.message}")
                        return@launch
                    }
                }
                
                // Formulate Shareable Summary before clearing
                val finalAbsentIds = studentsList.filter { !it.isSelected }.map { it.id }
                val presentStudents = allClassStudents.filter { it.id !in finalAbsentIds }
                val absentStudents = allClassStudents.filter { it.id in finalAbsentIds }
                
                val summary = buildString {
                    append("Attendance Summary - $subjectId ($period)\n\n")
                    append("✅ PRESENT (${presentStudents.size}):\n")
                    presentStudents.forEach { append("- ${it.name} (${it.roll_no})\n") }
                    append("\n❌ ABSENT (${absentStudents.size}):\n")
                    absentStudents.forEach { append("- ${it.name} (${it.roll_no})\n") }
                }
                
                shareableSummary = summary
                
                _result.postValue("Attendance Finalized Successfully")
                // Clear list or reset
                studentsList = emptyList()
                attendanceStep = "Marking"
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
