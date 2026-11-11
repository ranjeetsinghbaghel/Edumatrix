package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.domain.model.*

interface AdminRepository {
    suspend fun getStudents(): List<com.auraface.auraface_app.domain.model.Student>
    suspend fun getAttendance(): List<com.auraface.auraface_app.domain.model.Attendance>
    suspend fun getDashboardAnalytics(): com.auraface.auraface_app.data.network.model.DashboardAnalytics
    suspend fun getDefaulterCount(): com.auraface.auraface_app.data.network.model.DefaulterCount
    suspend fun getMostAbsentSubjects(): List<com.auraface.auraface_app.data.network.model.SubjectAbsence>
    suspend fun getDetailedReports(): com.auraface.auraface_app.data.network.model.AdminReportData
    suspend fun getTimetable(department: String, year: Int, section: String, semester: Int?): List<com.auraface.auraface_app.data.network.model.TimetableItem>
    suspend fun addTimetableSlot(slot: com.auraface.auraface_app.data.network.model.TimetableSlotCreate)
    suspend fun deleteTimetableSlot(id: Int)
    suspend fun updateTimetableSlot(id: Int, slot: com.auraface.auraface_app.data.network.model.TimetableSlotCreate)
    suspend fun sendNotice(notice: com.auraface.auraface_app.data.remote.dto.NoticeCreateDto)
    suspend fun notifyDefaulters(studentIds: List<Int>): Map<String, Any>
    suspend fun addCalendarEvent(title: String, dateStr: String, type: String): com.auraface.auraface_app.data.network.api.CalendarEventDTO
    suspend fun deleteCalendarEvent(id: Int): Map<String, String>
    suspend fun getAcademicCalendar(): List<com.auraface.auraface_app.data.network.api.CalendarEventDTO>
}
