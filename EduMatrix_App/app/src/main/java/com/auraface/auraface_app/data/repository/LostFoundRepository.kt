package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.LostFoundApi
import com.auraface.auraface_app.data.network.model.LostItemCreate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LostFoundRepository @Inject constructor(
    private val api: LostFoundApi
) {
    suspend fun getAllItems() = api.getAllItems()
    suspend fun reportItem(request: LostItemCreate) = api.reportItem(request)
    suspend fun resolveItem(itemId: String) = api.resolveItem(itemId)
}
