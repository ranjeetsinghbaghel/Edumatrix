package com.auraface.auraface_app.data.network.model

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val message: String,
    val username: String,
    val role: String
)
