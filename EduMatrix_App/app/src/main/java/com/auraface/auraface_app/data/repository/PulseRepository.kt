package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.PulseApi
import com.auraface.auraface_app.data.network.model.MoodCheckInCreate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PulseRepository @Inject constructor(
    private val api: PulseApi
) {
    suspend fun recordDailyMood(request: MoodCheckInCreate) = api.recordDailyMood(request)
    
    suspend fun getMyMoodHistory() = api.getMyMoodHistory()
    
    suspend fun getCampusInsights(department: String? = null, year: Int? = null, targetDate: String? = null) = 
        api.getCampusInsights(department, year, targetDate)
}
