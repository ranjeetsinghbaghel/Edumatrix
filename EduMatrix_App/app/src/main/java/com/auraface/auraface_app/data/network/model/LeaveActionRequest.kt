package com.auraface.auraface_app.data.network.model

data class LeaveActionRequest(
    val leave_id: Int,
    val action: String // "approve" or "reject"
)
