package com.auraface.auraface_app.data.remote.dto

data class GalleryFolderDto(
    val id: Int,
    val name: String,
    val created_at: String
)

data class GalleryImageDto(
    val id: Int,
    val folder_id: Int,
    val image_url: String,
    val uploaded_by_id: Int,
    val status: String, // PENDING, APPROVED
    val created_at: String
)
