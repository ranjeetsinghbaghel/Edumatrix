package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.GalleryApi
import com.auraface.auraface_app.data.remote.dto.GalleryFolderDto
import com.auraface.auraface_app.data.remote.dto.GalleryImageDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class GalleryRepository @Inject constructor(
    private val api: GalleryApi
) {

    suspend fun getFolders(): List<GalleryFolderDto> = api.getFolders()

    suspend fun createFolder(name: String) = 
        api.createFolder(name.toRequestBody("text/plain".toMediaTypeOrNull()))

    suspend fun deleteFolder(id: Int) = api.deleteFolder(id)

    suspend fun getImages(folderId: Int): List<GalleryImageDto> = api.getImages(folderId)

    suspend fun uploadImage(folderId: Int, file: File) {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val folderIdBody = folderId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        
        api.uploadImage(folderIdBody, body)
    }

    suspend fun approveImage(id: Int) = api.approveImage(id)

    suspend fun deleteImage(id: Int) = api.deleteImage(id)
}
