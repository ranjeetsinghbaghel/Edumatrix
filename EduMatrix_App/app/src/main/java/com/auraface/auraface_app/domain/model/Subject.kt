package com.auraface.auraface_app.domain.model

data class Subject(
    val id: String,
    val name: String,
    val teacher_id: Int? = null,
    val department: String? = null,
    val year: Int? = null,
    val semester: Int? = null,
    val section: String? = null
)
