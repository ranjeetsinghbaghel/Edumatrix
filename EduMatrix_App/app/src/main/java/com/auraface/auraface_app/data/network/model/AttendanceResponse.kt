package com.auraface.auraface_app.data.network.model

data class AttendanceResponse(
    val success: Boolean = true,
    val message: String,
    val student_id: String? = null,
    val student_name: String? = null,
    val status: String? = null
)
