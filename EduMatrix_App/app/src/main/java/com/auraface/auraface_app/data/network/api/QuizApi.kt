package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.GamificationProfile
import com.auraface.auraface_app.data.network.model.LeaderboardEntry
import com.auraface.auraface_app.data.network.model.QuestionResponse
import com.auraface.auraface_app.data.network.model.QuestionCreateRequest
import com.auraface.auraface_app.data.network.model.QuizAttemptSubmit
import com.auraface.auraface_app.data.network.model.QuizResultResponse
import com.auraface.auraface_app.data.network.model.RewardItemOut
import com.auraface.auraface_app.data.network.model.RedeemedRewardOut
import com.auraface.auraface_app.data.network.model.RedeemRequest
import com.auraface.auraface_app.data.network.model.DuelWagerCreate
import com.auraface.auraface_app.data.network.model.DuelInviteResponse
import com.auraface.auraface_app.data.network.model.FocusSessionSubmit
import com.auraface.auraface_app.data.network.model.FocusResultResponse
import com.auraface.auraface_app.data.network.model.QuizUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Multipart
import retrofit2.http.Query

@JvmSuppressWildcards
interface QuizApi {

    @GET("quiz/daily")
    suspend fun getDailyQuiz(): Response<List<QuestionResponse>>

    @POST("quiz/submit")
    suspend fun submitQuiz(@Body attempt: QuizAttemptSubmit): Response<QuizResultResponse>

    @GET("quiz/profile")
    suspend fun getProfile(): Response<GamificationProfile>

    @GET("quiz/leaderboard")
    suspend fun getLeaderboard(@Query("period") period: String = "all_time"): Response<List<LeaderboardEntry>>

    @POST("quiz/question")
    suspend fun createQuestion(@Body question: QuestionCreateRequest): Response<QuestionResponse>
    
    @Multipart
    @POST("quiz/upload")
    suspend fun uploadAttachment(@Part file: MultipartBody.Part): Response<QuizUploadResponse>

    @GET("quiz/rewards")
    suspend fun getRewards(): Response<List<RewardItemOut>>

    @POST("quiz/rewards/redeem")
    suspend fun redeemReward(@Body request: RedeemRequest): Response<RedeemedRewardOut>

    @GET("quiz/rewards/my")
    suspend fun getMyRewards(): Response<List<RedeemedRewardOut>>

    @POST("quiz/duel/invite")
    suspend fun sendDuelInvite(@Body request: DuelWagerCreate): Response<DuelInviteResponse>

    @POST("quiz/focus/submit")
    suspend fun submitFocusSession(@Body request: FocusSessionSubmit): Response<FocusResultResponse>

    @GET("quiz/mock-test/questions")
    suspend fun getMockTestQuestions(@Query("category") category: String): Response<List<QuestionResponse>>

    @POST("quiz/mock-test/submit")
    suspend fun submitMockTest(@Body body: Map<String, Any>): Response<com.auraface.auraface_app.data.remote.dto.MockTestResultDto>
}
