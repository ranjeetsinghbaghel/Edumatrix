package com.auraface.auraface_app.data.network.model

data class QuestOut(
    val id: String,
    val title: String,
    val description: String,
    val xp_reward: Int,
    val icon: String,
    val bg_color: String,
    val is_completed: Boolean,
    val is_claimed: Boolean
)

data class ClaimQuestResponse(
    val message: String,
    val xp_awarded: Int,
    val new_total_xp: Int,
    val level_up: Boolean
)
