package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.domain.model.Student
import com.auraface.auraface_app.data.network.api.StudentApi
import javax.inject.Inject

class StudentRepository @Inject constructor(
    private val api: StudentApi
) {
    suspend fun getStudents(): List<Student> = emptyList() // api.getStudents()
    suspend fun deleteStudent(id: Int) {} // api.deleteStudent(id)
    
    suspend fun getProfile(): Student = api.getProfile()
    suspend fun getMyAttendance(): List<com.auraface.auraface_app.domain.model.Attendance> {
        return try {
            api.getMyAttendance().map { 
                com.auraface.auraface_app.domain.model.Attendance(
                    id = it.id, 
                    subject = it.subject, 
                    date = it.date,
                    period = it.period,
                    status = it.status
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun submitLeave(reason: String): Map<String, String> {
        return api.submitLeave(mapOf("reason" to reason))
    }

    suspend fun updateBloodGroup(bloodGroup: String): Map<String, String> {
        return api.updateBloodGroup(mapOf("blood_group" to bloodGroup))
    }
    
    suspend fun getMyLeaves(): List<Map<String, Any>> {
        return try {
            api.getMyLeaves().map { dto ->
                mapOf(
                    "id" to dto.id,
                    "reason" to (dto.reason ?: ""),
                    "date" to (dto.date ?: ""),
                    "status" to (dto.status ?: "")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun changePassword(newPassword: String): Map<String, String> = emptyMap() // api.changePassword(mapOf("new_password" to newPassword))

    suspend fun getTimetable(): List<com.auraface.auraface_app.data.network.model.TimetableItem> {
        return try {
            api.getTimetable().map {
                com.auraface.auraface_app.data.network.model.TimetableItem(
                    id = it.id,
                    day = it.day ?: "",
                    time = it.time ?: "",
                    subject = it.subject ?: "Unknown",
                    room = it.room ?: "",
                    period = it.period,
                    date = it.date
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getNextClass(): com.auraface.auraface_app.data.network.model.TimetableItem? {
        return try {
            val dto = api.getNextClass()
            if (dto != null) {
                com.auraface.auraface_app.data.network.model.TimetableItem(
                    id = dto.id,
                    day = dto.day ?: "",
                    time = dto.time ?: "",
                    subject = dto.subject ?: "Unknown",
                    room = dto.room ?: "",
                    period = dto.period,
                    date = dto.date
                ) 
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getPersonalAnalytics(): com.auraface.auraface_app.data.network.model.StudentAnalytics? {
        return null
    }

    suspend fun getExamSchedule(): List<com.auraface.auraface_app.data.network.api.ExamScheduleDTO> {
        return try {
            api.getExamSchedule() 
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMyResults(): List<com.auraface.auraface_app.data.network.api.ResultDTO> {
        return try {
            api.getMyResults() 
        } catch (e: Exception) {
             e.printStackTrace()
             emptyList()
        }
    }
    
    suspend fun getMyCGPA(): com.auraface.auraface_app.data.network.api.GpaDTO? {
        return try {
            api.getMyCGPA() 
        } catch (e: Exception) {
            e.printStackTrace()
            null
            null
        }
    }

    suspend fun getNotifications(): List<com.auraface.auraface_app.data.network.model.Notification> {
        return try {
            api.getNotifications()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getMyProctorMeetings(): List<com.auraface.auraface_app.data.network.model.ProctorMeeting> {
        return try {
            api.getMyProctorMeetings()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendMessageToClassTeacher(message: String) {
        api.sendMessageToClassTeacher(mapOf("message" to message))
    }

    suspend fun respondToProctorMeeting(id: Int, response: String) {
        api.respondToProctorMeeting(id, mapOf("response" to response))
    }

    suspend fun getTeacherAvailability(): List<com.auraface.auraface_app.data.network.api.TeacherAvailabilityDTO> {
        return try {
            api.getTeacherAvailability()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAcademicCalendar(): List<com.auraface.auraface_app.data.network.api.CalendarEventDTO> {
         return try {
             api.getAcademicCalendar()
         } catch(e: Exception) {
             e.printStackTrace()
             emptyList()
         }
    }
}
