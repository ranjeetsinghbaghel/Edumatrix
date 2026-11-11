package com.auraface.auraface_app.data.network.model

data class AttendanceTrend(
    val day: String,
    val count: Int
)

data class RoleAnalytics(
    val totalStudents: Int,
    val totalTeachers: Int,
    val activeClasses: Int
)

data class DashboardAnalytics(
    val roleAnalytics: RoleAnalytics,
    val attendanceTrends: List<AttendanceTrend>
)

data class DefaulterStudent(
    val id: Int,
    val name: String,
    val department: String,
    val year: Int,
    val section: String,
    val rollNo: String,
    val percentage: Double,
    val guardian_name: String? = null,
    val guardian_mobile: String? = null,
    val guardian_email: String? = null
)

data class DefaulterCount(
    val defaulters: Int,
    val safe: Int,
    val students: List<DefaulterStudent>? = null
)

data class SubjectAbsence(
    val subject: String,
    val percentage: Double
)

