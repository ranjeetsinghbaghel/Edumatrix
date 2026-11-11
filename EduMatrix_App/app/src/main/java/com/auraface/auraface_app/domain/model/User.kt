package com.auraface.auraface_app.domain.model

data class User(
    val id: String,
    val username: String,
    val role: String   // ADMIN, TEACHER, STUDENT
)
