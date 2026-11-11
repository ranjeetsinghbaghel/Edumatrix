package com.auraface.auraface_app.data.network.model

data class ChatGroup(
    val group_id: String,
    val name: String,
    val department: String?,
    val year: Int?,
    val section: String?,
    val last_message: ChatMessageDto?,
    val unread_count: Int = 0,
    val profile_image: String? = null
)

data class UpdateGroupMetadataRequest(
    val name: String
)

data class ChatMessageDto(
    val id: Int,
    val sender_id: Int,
    val sender_name: String?,
    val sender_profile_image: String? = null,
    val group_id: String,
    val content: String?,
    val msg_type: String = "TEXT", // TEXT, IMAGE
    val attachment_url: String? = null,
    val filename: String? = null,
    val timestamp: String, // ISO format
    val reply_to_id: Int? = null,
    val status: String = "SENT", // SENT, DELIVERED, READ
    val is_deleted: Boolean = false
)

data class ChatMessageRequest(
    val group_id: String,
    val content: String,
    val msg_type: String = "TEXT"
)

data class ChatContact(
    val id: Int,
    val name: String,
    val role: String
)
