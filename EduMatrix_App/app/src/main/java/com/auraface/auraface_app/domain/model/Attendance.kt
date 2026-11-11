package com.auraface.auraface_app.domain.model

data class Attendance(
    val id: Int = 0,
    val subject: String? = null,
    val date: String? = null,
    val period: String? = null,
    val status: String? = "Present"
)
