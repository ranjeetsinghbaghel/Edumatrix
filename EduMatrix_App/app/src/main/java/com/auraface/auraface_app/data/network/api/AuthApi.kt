package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.remote.dto.LoginResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface AuthApi {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): LoginResponse

    @retrofit2.http.GET("auth/profile")
    suspend fun getProfile(): com.auraface.auraface_app.data.remote.dto.UserProfile

    @retrofit2.http.POST("auth/forgot-password")
    suspend fun forgotPassword(@retrofit2.http.Body body: Map<String, String>): Map<String, String>

    @retrofit2.http.POST("auth/verify-otp")
    suspend fun verifyOtp(@retrofit2.http.Body body: Map<String, String>): Map<String, String>

    @retrofit2.http.POST("auth/reset-password-with-otp")
    suspend fun resetPassword(@retrofit2.http.Body body: Map<String, String>): Map<String, String>

    @retrofit2.http.POST("auth/update-fcm-token")
    suspend fun updateFcmToken(@retrofit2.http.Body body: Map<String, String>): Map<String, String>
}
