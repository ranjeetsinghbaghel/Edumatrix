package com.auraface.auraface_app.data.network.model

data class AttendanceRequest(
    val image_base64: String,
    val subject_id: String,
    val period: String,
    val start_time: String? = null,
    val end_time: String? = null
)
