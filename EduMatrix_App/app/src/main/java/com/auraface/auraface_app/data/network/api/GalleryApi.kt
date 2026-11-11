package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.remote.dto.GalleryFolderDto
import com.auraface.auraface_app.data.remote.dto.GalleryImageDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface GalleryApi {

    @GET("/gallery/folders")
    suspend fun getFolders(): List<GalleryFolderDto>

    @Multipart
    @POST("/gallery/folders")
    suspend fun createFolder(
        @Part("name") name: RequestBody
    ): GalleryFolderDto
    
    @DELETE("/gallery/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: Int)

    @GET("/gallery/folders/{folderId}/images")
    suspend fun getImages(@Path("folderId") folderId: Int): List<GalleryImageDto>

    @Multipart
    @POST("/gallery/upload")
    suspend fun uploadImage(
        @Part("folder_id") folderId: RequestBody,
        @Part image: MultipartBody.Part
    ): GalleryImageDto

    @PUT("/gallery/images/{id}/approve")
    suspend fun approveImage(@Path("id") id: Int)
    
    @DELETE("/gallery/images/{id}")
    suspend fun deleteImage(@Path("id") id: Int)
}
