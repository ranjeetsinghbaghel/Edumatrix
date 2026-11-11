package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SmartFeaturesApi {

    // --- Notices ---
    @GET("notifications/notices")
    suspend fun getNotices(): List<NoticeDto>

    @POST("notifications/notices/{id}/read")
    suspend fun markNoticeRead(@Path("id") id: Int): Map<String, String>

    @GET("notifications/notices/{id}/readers")
    suspend fun getNoticeReaders(@Path("id") id: Int): List<NoticeReaderDto>

    // --- Insights ---
    @GET("insights/trend")
    suspend fun getTrend(): List<TrendPointDto>

    @GET("insights/risk")
    suspend fun getRisk(): StudentRiskDto

    @GET("insights/required")
    suspend fun getRequiredClasses(): RequiredClassesDto

    // --- Chat ---
    @POST("chat/ask")
    suspend fun askAura(@Body message: ChatRequestDto): ChatResponseDto
}
