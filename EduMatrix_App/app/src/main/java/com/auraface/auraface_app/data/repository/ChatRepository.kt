package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.model.ChatMessageDto 
import com.auraface.auraface_app.data.network.api.ChatApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@Singleton
class ChatRepository @Inject constructor(
    private val api: ChatApi
) {
    suspend fun getMyGroups() = api.getMyGroups()

    suspend fun getContacts() = api.getContacts()

    suspend fun getChatHistory(groupId: String) = api.getChatHistory(groupId)

    suspend fun uploadFile(file: File) =
        api.uploadAttachment(
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        )

    suspend fun deleteMessage(id: Int) = api.deleteMessage(id)
    suspend fun editMessage(id: Int, content: String) = api.editMessage(id, com.auraface.auraface_app.data.network.api.EditRequest(content))

    suspend fun deleteGroup(groupId: String) = api.deleteGroup(groupId)
    
    suspend fun updateGroupMetadata(groupId: String, name: String) = 
        api.updateGroupMetadata(groupId, com.auraface.auraface_app.data.network.model.UpdateGroupMetadataRequest(name))
        
    suspend fun uploadGroupImage(groupId: String, file: File) =
        api.uploadGroupImage(
            groupId,
            MultipartBody.Part.createFormData(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        )
}
