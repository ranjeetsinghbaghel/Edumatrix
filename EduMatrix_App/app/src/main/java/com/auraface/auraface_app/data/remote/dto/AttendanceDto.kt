package com.auraface.auraface_app.data.remote.dto

data class AttendanceRequest(
    val studentId: Int,
    val subjectId: Int,
    val date: String,
    val status: String = "PRESENT"
)

data class AttendanceResponse(
    val success: Boolean,
    val message: String,
    val marked: Boolean = true
)
