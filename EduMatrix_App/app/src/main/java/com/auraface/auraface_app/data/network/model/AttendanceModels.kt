package com.auraface.auraface_app.data.network.model

data class BulkAttendanceMark(
    val student_ids: List<Int>,
    val subject_id: String,
    val period: String
)

data class AttendanceStudent(
    val id: Int,
    val name: String,
    val roll_no: String,
    val department: String,
    val year: Int,
    val section: String,
    var isSelected: Boolean = false // Mutable for UI
)
