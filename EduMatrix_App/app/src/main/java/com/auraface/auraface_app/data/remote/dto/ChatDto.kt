package com.auraface.auraface_app.data.remote.dto

data class ChatMessageDto(
    val id: Int,
    val sender_id: Int,
    val sender_name: String?,
    val group_id: String,
    val content: String?,
    val msg_type: String = "TEXT",
    val attachment_url: String?,
    val timestamp: String,
    val reply_to_id: Int?,
    val status: String? = "DELIVERED",
    val is_deleted: Boolean = false
)

data class ChatGroupDto(
    val group_id: String,
    val name: String,
    val department: String?,
    val year: Int?,
    val section: String?,
    val last_message: ChatMessageDto?,
    val unread_count: Int
)

data class ChatUploadResponse(
    val url: String,
    val filename: String? = null,
    val size: String? = null
)
