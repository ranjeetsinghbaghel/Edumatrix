package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.LostItemCreate
import com.auraface.auraface_app.data.network.model.LostItemOut
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LostFoundApi {
    @GET("lost-found/list")
    suspend fun getAllItems(): Response<List<LostItemOut>>

    @POST("lost-found/report")
    suspend fun reportItem(@Body request: LostItemCreate): Response<LostItemOut>

    @PUT("lost-found/resolve/{itemId}")
    suspend fun resolveItem(@Path("itemId") itemId: String): Response<LostItemOut>
}
