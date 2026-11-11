package com.auraface.auraface_app.data.network.model

data class MakeupClassCreate(
    val date: String, // YYYY-MM-DD
    val time_slot: String, // HH:MM-HH:MM
    val subject_id: String,
    val department: String,
    val year: Int,
    val semester: Int,
    val section: String,
    val room: String? = null,
    val reason: String? = null
)
