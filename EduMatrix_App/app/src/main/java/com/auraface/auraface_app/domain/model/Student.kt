package com.auraface.auraface_app.domain.model

data class Student(
    val id: Int = 0,
    val name: String,
    val roll_no: String,
    val program: String? = null,
    val department: String,
    val year: Int,
    val semester: Int? = null,
    val section: String,
    val email: String? = null,
    val mobile: String? = null,
    val guardian_name: String? = null,
    val guardian_email: String? = null,
    val guardian_mobile: String? = null,
    val profile_image: String? = null,
    val password: String? = null
)
