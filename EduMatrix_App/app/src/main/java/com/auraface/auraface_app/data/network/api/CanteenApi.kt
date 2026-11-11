package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.CanteenStatus
import com.auraface.auraface_app.data.network.model.MenuItem
import com.auraface.auraface_app.data.network.model.OrderRequest
import com.auraface.auraface_app.data.network.model.OrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CanteenApi {
    @GET("canteen/status")
    suspend fun getStatus(): Response<CanteenStatus>

    @GET("canteen/menu")
    suspend fun getMenu(): Response<List<MenuItem>>

    @POST("canteen/order")
    suspend fun placeOrder(@Body request: OrderRequest): Response<OrderResponse>
}
