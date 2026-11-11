package com.auraface.auraface_app.data.network.model

data class YearlyReport(
    val year: Int,
    val monthlyData: List<MonthlyTrend>
)

data class MonthlyTrend(
    val month: String,
    val averageAttendance: Float
)

data class SubjectReport(
    val subjectName: String,
    val totalClasses: Int,
    val averageAttendance: Float
)

data class AdminReportData(
    val yearlyTrends: List<YearlyReport>,
    val subjectPerformance: List<SubjectReport>
)
