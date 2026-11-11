package com.auraface.auraface_app.domain.model

data class Teacher(
    val id: Int = 0,
    val username: String,
    val full_name: String? = null,
    val password: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    val qualification: String? = null,
    val profile_image: String? = null
)
