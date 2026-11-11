package com.auraface.auraface_app.data.network.model

data class TimetableSlotCreate(
    val department: String,
    val year: Int,
    val semester: Int? = null,
    val section: String,
    val day_of_week: String,
    val time_slot: String,
    val subject: String,
    val teacher_id: Int? = null,
    val period: String? = "1",
    val room: String? = null,
    val date: String? = null,
    val status: String? = null
)
