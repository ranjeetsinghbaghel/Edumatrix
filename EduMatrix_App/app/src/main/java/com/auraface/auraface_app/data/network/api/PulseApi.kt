package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PulseApi {
    @POST("pulse/checkin")
    suspend fun recordDailyMood(@Body request: MoodCheckInCreate): Response<MoodCheckInOut>

    @GET("pulse/my-history")
    suspend fun getMyMoodHistory(): Response<List<MoodCheckInOut>>

    @GET("pulse/insights")
    suspend fun getCampusInsights(
        @Query("department") department: String? = null,
        @Query("year") year: Int? = null,
        @Query("target_date") targetDate: String? = null
    ): Response<PulseDashboardOut>
}
