package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.QuestApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestRepository @Inject constructor(
    private val api: QuestApi
) {
    suspend fun getMyQuests() = api.getMyQuests()
    suspend fun claimQuest(questId: String) = api.claimQuest(questId)
}
