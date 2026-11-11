package com.auraface.auraface_app.data.network.model

data class ManualAttendanceRequest(
    val roll_no: String,
    val subject_id: String,
    val period: String,
    val start_time: String? = null,
    val end_time: String? = null
)
