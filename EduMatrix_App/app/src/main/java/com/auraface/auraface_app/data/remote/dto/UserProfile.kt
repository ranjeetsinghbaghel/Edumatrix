package com.auraface.auraface_app.data.remote.dto

data class UserProfile(
    val username: String,
    val name: String? = null,
    val role: String,
    val mobile: String? = null,
    val email: String? = null,
    val address: String? = null,
    val position: String? = null,
    val profile_image: String? = null,
    val department: String? = null,
    val year: Int? = null,
    val semester: Int? = null,
    val section: String? = null,
    val roll_no: String? = null,
    val program: String? = null,
    val id: Int? = null,
    val guardian_name: String? = null,
    val guardian_email: String? = null,
    val guardian_mobile: String? = null,
    val qualification: String? = null,
    val blood_group: String? = null
)
