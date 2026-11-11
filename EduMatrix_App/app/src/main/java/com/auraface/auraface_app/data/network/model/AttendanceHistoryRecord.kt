package com.auraface.auraface_app.data.network.model

data class AttendanceHistoryRecord(
    val date: String,
    val period: String,
    val total_present: Int,
    val total_absent: Int,
    val present: List<Map<String, String>> = emptyList(),
    val absent: List<Map<String, String>> = emptyList()
)
