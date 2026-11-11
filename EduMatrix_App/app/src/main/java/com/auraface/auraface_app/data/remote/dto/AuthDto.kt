package com.auraface.auraface_app.data.remote.dto

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val username: String,
    val role: String, // admin / teacher / student
    val is_hod: Boolean? = false,
    val is_class_teacher: Boolean? = false
)
