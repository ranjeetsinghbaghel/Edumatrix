package com.auraface.auraface_app.data.network.model

data class QuestionResponse(
    val id: Int,
    val category: String,
    val subcategory: String?,
    val difficulty: String,
    val question_text: String,
    val attachment_url: String? = null,
    val options: List<String>
)

data class QuizAttemptSubmit(
    val answers: Map<Int, Int>, // QuestionId -> SelectedIndex
    val time_taken_seconds: Int
)

data class QuizResultResponse(
    val score: Int,
    val total_questions: Int,
    val xp_earned: Int,
    val new_total_xp: Int,
    val new_level: Int,
    val streak_bonus: Int,
    val next_level_xp: Int,
    val progress: Float,
    val correct_answers: Map<Int, Int>, // QuestionId -> CorrectIndex
    val explanations: Map<Int, String>, // QuestionId -> Explanation
    val actual_marks: Float? = null,
    val percentage: Float? = null,
    val grade: String? = null,
    val marks_recorded: Boolean? = true
)

data class GamificationProfile(
    val user_id: Int,
    val total_xp: Int,
    val current_level: Int,
    val current_streak: Int,
    val badges: List<String>,
    val title: String,
    val next_level_xp: Int,
    val progress: Float // 0.0 to 1.0
)

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val full_name: String,
    val total_xp: Int,
    val badges: List<String>
)

data class QuestionCreateRequest(
    val category: String,
    val subcategory: String? = null,
    val difficulty: String = "MEDIUM",
    val question_text: String,
    val attachment_url: String? = null,
    val options: List<String>,
    val correct_option_index: Int,
    val explanation: String? = null
)

data class QuizUploadResponse(
    val url: String,
    val filename: String
)

data class RewardItemOut(
    val id: Int,
    val title: String,
    val description: String,
    val xp_cost: Int,
    val icon_name: String,
    val bg_color: String,
    val stock: Int,
    val is_active: Boolean
)

data class RedeemRequest(
    val reward_id: Int
)

data class RedeemedRewardOut(
    val id: Int,
    val reward_id: Int,
    val redeemed_at: String, // Parsing date as string for now
    val status: String,
    val reward: RewardItemOut? = null
)

data class DuelWagerCreate(
    val target_user_id: Int,
    val wager_amount: Int,
    val category: String = "MIXED"
)

data class DuelInviteResponse(
    val message: String,
    val duel_id: Int
)

data class FocusSessionSubmit(
    val duration_minutes: Int,
    val subject_tag: String? = "General Study"
)

data class FocusResultResponse(
    val message: String,
    val xp_earned: Int,
    val new_total_xp: Int,
    val level_up: Boolean
)
