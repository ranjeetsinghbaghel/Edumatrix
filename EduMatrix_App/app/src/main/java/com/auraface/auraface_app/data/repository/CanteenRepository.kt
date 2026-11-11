package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.CanteenApi
import com.auraface.auraface_app.data.network.model.OrderRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanteenRepository @Inject constructor(
    private val api: CanteenApi
) {
    suspend fun getStatus() = api.getStatus()
    suspend fun getMenu() = api.getMenu()
    suspend fun placeOrder(request: OrderRequest) = api.placeOrder(request)
}
