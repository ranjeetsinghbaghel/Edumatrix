package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.domain.model.Attendance
import com.auraface.auraface_app.domain.model.Student
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val api: com.auraface.auraface_app.data.network.api.AdminApi
) : AdminRepository {

    override suspend fun getStudents(): List<Student> =
        listOf()

    override suspend fun getAttendance(): List<Attendance> =
        listOf()

    override suspend fun getDashboardAnalytics(): com.auraface.auraface_app.data.network.model.DashboardAnalytics {
        return api.getDashboardAnalytics()
    }

    override suspend fun getDefaulterCount(): com.auraface.auraface_app.data.network.model.DefaulterCount {
        return api.getDefaulters()
    }

    override suspend fun getMostAbsentSubjects(): List<com.auraface.auraface_app.data.network.model.SubjectAbsence> {
        return api.getMostAbsentSubjects()
    }

    override suspend fun getDetailedReports(): com.auraface.auraface_app.data.network.model.AdminReportData {
        return com.auraface.auraface_app.data.network.model.AdminReportData(
            yearlyTrends = listOf(
                com.auraface.auraface_app.data.network.model.YearlyReport(
                    year = 2025,
                    monthlyData = listOf(
                        com.auraface.auraface_app.data.network.model.MonthlyTrend("Jan", 85f),
                        com.auraface.auraface_app.data.network.model.MonthlyTrend("Feb", 88f),
                        com.auraface.auraface_app.data.network.model.MonthlyTrend("Mar", 82f),
                        com.auraface.auraface_app.data.network.model.MonthlyTrend("Apr", 75f),
                        com.auraface.auraface_app.data.network.model.MonthlyTrend("May", 90f)
                    )
                )
            ),
            subjectPerformance = listOf(
                com.auraface.auraface_app.data.network.model.SubjectReport("Mobile App Dev", 45, 88.5f),
                com.auraface.auraface_app.data.network.model.SubjectReport("Cloud Computing", 40, 72.0f),
                com.auraface.auraface_app.data.network.model.SubjectReport("Cyber Security", 38, 95.2f)
            )
        )
    }

    override suspend fun getTimetable(department: String, year: Int, section: String, semester: Int?) =
        api.getTimetable(department, year, section, semester).data ?: emptyList()

    override suspend fun addTimetableSlot(slot: com.auraface.auraface_app.data.network.model.TimetableSlotCreate) {
        val res = api.addTimetableSlot(slot)
        if (!res.success) throw Exception(res.error?.message ?: "Unknown error")
    }

    override suspend fun deleteTimetableSlot(id: Int) {
        val res = api.deleteTimetableSlot(id)
        if (!res.success) throw Exception(res.error?.message ?: "Unknown error")
    }

    override suspend fun updateTimetableSlot(id: Int, slot: com.auraface.auraface_app.data.network.model.TimetableSlotCreate) {
        val res = api.updateTimetableSlot(id, slot)
        if (!res.success) throw Exception(res.error?.message ?: "Unknown error")
    }

    override suspend fun sendNotice(notice: com.auraface.auraface_app.data.remote.dto.NoticeCreateDto) {
        api.sendNotice(notice)
    }

    override suspend fun notifyDefaulters(studentIds: List<Int>): Map<String, Any> {
        return api.notifyDefaulters(mapOf("student_ids" to studentIds))
    }

    override suspend fun addCalendarEvent(title: String, dateStr: String, type: String): com.auraface.auraface_app.data.network.api.CalendarEventDTO {
        return api.addCalendarEvent(mapOf("title" to title, "date_str" to dateStr, "event_type" to type))
    }

    override suspend fun deleteCalendarEvent(id: Int): Map<String, String> {
        return api.deleteCalendarEvent(id)
    }

    override suspend fun getAcademicCalendar(): List<com.auraface.auraface_app.data.network.api.CalendarEventDTO> {
        return api.getAcademicCalendar()
    }
}
