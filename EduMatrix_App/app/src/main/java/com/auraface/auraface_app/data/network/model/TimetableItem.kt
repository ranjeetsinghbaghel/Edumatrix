package com.auraface.auraface_app.data.network.model

data class TimetableItem(
    val id: Int,
    val day: String?,
    val time: String?,
    val subject: String,
    val subjectName: String? = null,
    val teacher: String? = null, // Student view sees teacher
    val teacherId: Int? = null, // Teacher ID for editing
    val department: String? = null, // Teacher view sees class details
    val year: Int? = null,
    val semester: Int? = null,
    val section: String? = null,
    val period: String? = null,
    val room: String? = null,
    val date: String? = null,
    val status: String? = null,
    val requestReason: String? = null
)
