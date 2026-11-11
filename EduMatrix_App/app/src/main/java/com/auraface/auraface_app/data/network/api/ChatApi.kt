package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.ChatGroup
import com.auraface.auraface_app.data.network.model.ChatMessageDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {

    @GET("chat/groups")
    suspend fun getMyGroups(): Response<List<ChatGroup>>

    @GET("chat/contacts")
    suspend fun getContacts(): Response<List<com.auraface.auraface_app.data.network.model.ChatContact>>

    @GET("chat/history/{group_id}")
    suspend fun getChatHistory(
        @Path("group_id") groupId: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<ChatMessageDto>>

    @Multipart
    @POST("chat/upload")
    suspend fun uploadAttachment(
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>

    @DELETE("chat/messages/{id}")
    suspend fun deleteMessage(@Path("id") id: Int): Response<Unit>

    
    @PUT("chat/messages/{id}")
    suspend fun editMessage(@Path("id") id: Int, @Body req: EditRequest): Response<Unit>

    @DELETE("chat/groups/{group_id}")
    suspend fun deleteGroup(@Path("group_id") groupId: String): Response<Unit>

    @PUT("chat/groups/{group_id}/metadata")
    suspend fun updateGroupMetadata(
        @Path("group_id") groupId: String, 
        @Body req: com.auraface.auraface_app.data.network.model.UpdateGroupMetadataRequest
    ): Response<Unit>

    @Multipart
    @POST("chat/groups/{group_id}/profile_image")
    suspend fun uploadGroupImage(
        @Path("group_id") groupId: String,
        @Part file: MultipartBody.Part
    ): Response<Map<String, String>>
}

data class EditRequest(val content: String)
