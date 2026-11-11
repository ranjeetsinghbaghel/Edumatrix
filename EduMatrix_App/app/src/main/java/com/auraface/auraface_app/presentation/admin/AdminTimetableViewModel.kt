package com.auraface.auraface_app.presentation.admin

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.repository.AdminRepository
import com.auraface.auraface_app.data.network.model.TimetableItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject

@HiltViewModel
class AdminTimetableViewModel @Inject constructor(
    private val repo: AdminRepository,
    private val teacherRepo: com.auraface.auraface_app.data.repository.TeacherRepository
) : ViewModel() {

    var teachersList by mutableStateOf<List<com.auraface.auraface_app.domain.model.Teacher>>(emptyList())
        private set

    var selectedDepartment by mutableStateOf("")
    var selectedYear by mutableStateOf("")
    var selectedSemester by mutableStateOf("")
    var selectedSection by mutableStateOf("")

    var timetable by mutableStateOf<List<TimetableItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadTeachers()
    }

    fun loadTeachers() {
        viewModelScope.launch {
            try {
                teachersList = teacherRepo.getTeachers()
            } catch (e: Exception) {
                android.util.Log.e("AdminTimetableVM", "Failed to load teachers", e)
            }
        }
    }

    fun loadTimetable() {
        if (selectedDepartment.isBlank() || selectedYear.isBlank() || selectedSection.isBlank()) {
            android.util.Log.w("AdminTimetableVM", "Cannot load timetable: missing filters")
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val yearInt = selectedYear.toIntOrNull() ?: 0
                val semInt = selectedSemester.toIntOrNull()
                android.util.Log.d("AdminTimetableVM", "Loading timetable for: $selectedDepartment, Year $yearInt, Sem $semInt, Section $selectedSection")
                timetable = repo.getTimetable(selectedDepartment, yearInt, selectedSection, semInt)
                android.util.Log.d("AdminTimetableVM", "Timetable loaded: ${timetable.size} slots")
            } catch (e: Exception) {
                errorMessage = "Failed to load: ${e.message}"
                android.util.Log.e("AdminTimetableVM", "Error loading timetable: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun addSlot(
        day: String,
        time: String,
        subject: String,
        teacherId: Int?, 
        period: String,
        room: String?,
        semester: Int?,
        date: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
             try {
                val yearInt = selectedYear.toIntOrNull() ?: 0
                val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                    department = selectedDepartment,
                    year = yearInt,
                    semester = semester ?: selectedSemester.toIntOrNull(),
                    section = selectedSection,
                    day_of_week = day,
                    time_slot = time,
                    subject = subject,
                    teacher_id = teacherId,
                    period = period,
                    room = room?.ifBlank { null },
                    date = date?.ifBlank { null }
                )
                android.util.Log.d("AdminTimetableVM", "Adding slot: $subject on $day at $time")
                repo.addTimetableSlot(payload)
                android.util.Log.d("AdminTimetableVM", "Slot added successfully, reloading timetable...")
                loadTimetable()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to add slot: ${e.message}"
                android.util.Log.e("AdminTimetableVM", "Error adding slot: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteSlot(id: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                repo.deleteTimetableSlot(id)
                loadTimetable()
            } catch (e: Exception) {
                errorMessage = "Failed to delete: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateSlot(
        id: Int,
        day: String,
        time: String,
        subject: String,
        teacherId: Int?, 
        period: String,
        room: String?,
        semester: Int?,
        date: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
             try {
                val yearInt = selectedYear.toIntOrNull() ?: 0
                val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                    department = selectedDepartment,
                    year = yearInt,
                    semester = semester ?: selectedSemester.toIntOrNull(),
                    section = selectedSection,
                    day_of_week = day,
                    time_slot = time,
                    subject = subject,
                    teacher_id = teacherId,
                    period = period,
                    room = room?.ifBlank { null },
                    date = date?.ifBlank { null }
                )
                repo.updateTimetableSlot(id, payload)
                loadTimetable()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to update slot: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    fun approveSlot(item: TimetableItem) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Construct update payload with APPROVED status
                // We use item details or fallback to selected filters (since we are viewing that schedule)
                val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                    department = item.department ?: selectedDepartment,
                    year = item.year ?: (selectedYear.toIntOrNull() ?: 0),
                    semester = item.semester ?: selectedSemester.toIntOrNull(),
                    section = item.section ?: selectedSection,
                    day_of_week = item.day ?: "Monday",
                    time_slot = item.time ?: "",
                    subject = item.subject,
                    teacher_id = item.teacherId,
                    period = item.period ?: "1",
                    room = item.room?.ifBlank { null },
                    date = item.date?.ifBlank { null },
                    status = "APPROVED"
                )
                
                repo.updateTimetableSlot(item.id, payload)
                loadTimetable()
            } catch (e: Exception) {
                errorMessage = "Failed to approve slot: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    fun bulkUpdateWefDate(newDate: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Correctly map Date string formats preventing HTTP 422 Pydantic Validation errors
                // Backend requires ISO 8601 (YYYY-MM-DD), but UI suggests DD-MM-YYYY
                var safeIsoDate = newDate
                if (newDate.contains("-") && newDate.split("-").first().length <= 2) {
                    val parts = newDate.split("-")
                    if (parts.size == 3) {
                        val d = parts[0].padStart(2, '0')
                        val m = parts[1].padStart(2, '0')
                        val y = parts[2]
                        if (y.length == 4) safeIsoDate = "$y-$m-$d"
                    }
                }

                val itemsToUpdate = timetable.filter { it.date != safeIsoDate }
                if (itemsToUpdate.isEmpty() && timetable.isEmpty()) {
                    val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                        department = selectedDepartment,
                        year = selectedYear.toIntOrNull() ?: 0,
                        semester = selectedSemester.toIntOrNull(),
                        section = selectedSection,
                        day_of_week = "Monday",
                        time_slot = "Unknown Time",
                        subject = "Placeholder",
                        teacher_id = null,
                        period = "1",
                        room = null,
                        date = safeIsoDate?.ifBlank { null }
                    )
                    repo.addTimetableSlot(payload)
                } else {
                    itemsToUpdate.forEach { item ->
                        val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                            department = item.department ?: selectedDepartment,
                            year = item.year ?: (selectedYear.toIntOrNull() ?: 0),
                            semester = item.semester ?: selectedSemester.toIntOrNull(),
                            section = item.section ?: selectedSection,
                            day_of_week = item.day ?: "Monday",
                            time_slot = item.time ?: "",
                            subject = item.subject,
                            teacher_id = item.teacherId,
                            period = item.period ?: "1",
                            room = item.room?.ifBlank { null },
                            date = safeIsoDate?.ifBlank { null }
                        )
                        repo.updateTimetableSlot(item.id, payload)
                    }
                }
                loadTimetable()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to update WEF Date: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun bulkUpdateClassroom(newRoom: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val itemsToUpdate = timetable.filter { it.room != newRoom }
                if (itemsToUpdate.isEmpty() && timetable.isEmpty()) {
                    val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                        department = selectedDepartment,
                        year = selectedYear.toIntOrNull() ?: 0,
                        semester = selectedSemester.toIntOrNull(),
                        section = selectedSection,
                        day_of_week = "Monday",
                        time_slot = "",
                        subject = "",
                        teacher_id = null,
                        period = "1",
                        room = newRoom?.ifBlank { null },
                        date = null
                    )
                    repo.addTimetableSlot(payload)
                } else {
                    itemsToUpdate.forEach { item ->
                        val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                            department = item.department ?: selectedDepartment,
                            year = item.year ?: (selectedYear.toIntOrNull() ?: 0),
                            semester = item.semester ?: selectedSemester.toIntOrNull(),
                            section = item.section ?: selectedSection,
                            day_of_week = item.day ?: "Monday",
                            time_slot = item.time ?: "",
                            subject = item.subject,
                            teacher_id = item.teacherId,
                            period = item.period ?: "1",
                            room = newRoom?.ifBlank { null },
                            date = item.date?.ifBlank { null }
                        )
                        repo.updateTimetableSlot(item.id, payload)
                    }
                }
                loadTimetable()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to update Classroom: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun bulkUpdateSubjectAndTeacher(oldSubject: String, newSubject: String, newTeacherId: Int?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val itemsToUpdate = timetable.filter { it.subject == oldSubject }
                itemsToUpdate.forEach { item ->
                    val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                        department = item.department ?: selectedDepartment,
                        year = item.year ?: (selectedYear.toIntOrNull() ?: 0),
                        semester = item.semester ?: selectedSemester.toIntOrNull(),
                        section = item.section ?: selectedSection,
                        day_of_week = item.day ?: "Monday",
                        time_slot = item.time ?: "",
                        subject = newSubject.ifBlank { "Placeholder" },
                        teacher_id = newTeacherId,
                        period = item.period ?: "1",
                        room = item.room?.ifBlank { null },
                        date = item.date?.ifBlank { null }
                    )
                    repo.updateTimetableSlot(item.id, payload)
                }
                loadTimetable()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to update Subject: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun bulkUpdatePeriodTime(periodParam: String, newTime: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val altPeriod = when (periodParam) { "1" -> "I"; "2" -> "II"; "3" -> "III"; "4" -> "IV"; "5" -> "V"; "6" -> "VI"; else -> periodParam }
                val itemsToUpdate = timetable.filter { (it.period == periodParam || it.period?.uppercase() == altPeriod) && it.time != newTime }
                
                if (itemsToUpdate.isEmpty()) {
                    // If no slots exist yet for this period, we could add a dummy metadata slot
                    val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                        department = selectedDepartment,
                        year = selectedYear.toIntOrNull() ?: 0,
                        semester = selectedSemester.toIntOrNull(),
                        section = selectedSection,
                        day_of_week = "Monday",
                        time_slot = newTime,
                        subject = "", // Empty Subject indicates a dummy/metadata placeholder slot
                        teacher_id = null,
                        period = periodParam,
                        room = "",
                        date = timetable.mapNotNull { it.date?.takeIf { d -> d.isNotBlank() } }.firstOrNull() ?: ""
                    )
                    repo.addTimetableSlot(payload)
                } else {
                    itemsToUpdate.forEach { item ->
                        val payload = com.auraface.auraface_app.data.network.model.TimetableSlotCreate(
                            department = item.department ?: selectedDepartment,
                            year = item.year ?: (selectedYear.toIntOrNull() ?: 0),
                            semester = item.semester ?: selectedSemester.toIntOrNull(),
                            section = item.section ?: selectedSection,
                            day_of_week = item.day ?: "Monday",
                            time_slot = newTime,
                            subject = item.subject,
                            teacher_id = item.teacherId,
                            period = item.period ?: "1",
                            room = item.room?.ifBlank { null },
                            date = item.date?.ifBlank { null }
                        )
                        repo.updateTimetableSlot(item.id, payload)
                    }
                }
                
                loadTimetable()
                onSuccess()
            } catch (e: Exception) {
                errorMessage = "Failed to update Period time: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
