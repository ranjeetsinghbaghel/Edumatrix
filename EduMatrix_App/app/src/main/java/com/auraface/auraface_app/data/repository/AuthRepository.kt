package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.AuthApi
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun login(username: String, password: String) =
        authApi.login(username = username, password = password)

    suspend fun getProfile() = authApi.getProfile()

    suspend fun forgotPassword(username: String) = authApi.forgotPassword(mapOf("username" to username))

    suspend fun verifyOtp(username: String, otp: String) = authApi.verifyOtp(mapOf("username" to username, "otp" to otp))

    suspend fun resetPassword(username: String, otp: String, newPassword: String) = 
        authApi.resetPassword(mapOf("username" to username, "otp" to otp, "new_password" to newPassword))

    suspend fun updateFcmToken(token: String) = 
        authApi.updateFcmToken(mapOf("fcm_token" to token))
}
