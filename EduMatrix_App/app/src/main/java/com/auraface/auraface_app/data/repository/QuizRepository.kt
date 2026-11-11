package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.QuizApi
import com.auraface.auraface_app.data.network.model.QuizAttemptSubmit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val api: QuizApi
) {
    suspend fun getDailyQuiz() = api.getDailyQuiz()
    
    suspend fun submitQuiz(attempt: QuizAttemptSubmit) = api.submitQuiz(attempt)
    
    suspend fun getProfile() = api.getProfile()
    
    
    suspend fun getLeaderboard(period: String = "all_time") = api.getLeaderboard(period)

    suspend fun createQuestion(request: com.auraface.auraface_app.data.network.model.QuestionCreateRequest) = api.createQuestion(request)
    
    suspend fun uploadAttachment(file: okhttp3.MultipartBody.Part) = api.uploadAttachment(file)

    suspend fun getRewards() = api.getRewards()

    suspend fun redeemReward(request: com.auraface.auraface_app.data.network.model.RedeemRequest) = api.redeemReward(request)

    suspend fun getMyRewards() = api.getMyRewards()

    suspend fun sendDuelInvite(request: com.auraface.auraface_app.data.network.model.DuelWagerCreate) = api.sendDuelInvite(request)

    suspend fun submitFocusSession(request: com.auraface.auraface_app.data.network.model.FocusSessionSubmit) = api.submitFocusSession(request)
}
