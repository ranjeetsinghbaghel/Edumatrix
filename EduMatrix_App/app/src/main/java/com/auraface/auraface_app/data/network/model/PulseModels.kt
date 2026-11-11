package com.auraface.auraface_app.data.network.model

data class MoodCheckInCreate(
    val mood: String,
    val notes: String? = null
)

data class MoodCheckInOut(
    val id: Int,
    val user_id: Int,
    val date: String,
    val mood: String,
    val notes: String?,
    val timestamp: String,
    val xp_rewarded: Int? = 0
)

data class DailyPulseInsight(
    val mood: String,
    val count: Int,
    val percentage: Float
)

data class PulseDashboardOut(
    val total_students: Int,
    val dominant_mood: String,
    val insights: List<DailyPulseInsight>
)
