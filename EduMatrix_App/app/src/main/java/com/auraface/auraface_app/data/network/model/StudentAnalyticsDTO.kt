package com.auraface.auraface_app.data.network.model

data class SubjectAttendance(
    val subjectName: String,
    val attendedClasses: Int,
    val totalClasses: Int,
    val percentage: Float
)

data class StudentAnalytics(
    val overallAttendance: Float,
    val subjectWiseAttendance: List<SubjectAttendance>,
    val recentAttendance: List<AttendanceTrend>
)
