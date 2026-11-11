package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.QuestOut
import com.auraface.auraface_app.data.network.model.ClaimQuestResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface QuestApi {
    @GET("quiz/quests/my")
    suspend fun getMyQuests(): Response<List<QuestOut>>

    @POST("quiz/quests/claim/{questId}")
    suspend fun claimQuest(@Path("questId") questId: String): Response<ClaimQuestResponse>
}
