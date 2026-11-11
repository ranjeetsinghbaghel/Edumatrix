package com.auraface.auraface_app.data.network.model

data class LostItemCreate(
    val title: String,
    val description: String,
    val location_found_or_lost: String,
    val type: String, // "LOST" or "FOUND"
    val category: String // "Electronics", "ID/Wallet", "Keys", "Other"
)

data class LostItemOut(
    val id: String,
    val title: String,
    val description: String,
    val location_found_or_lost: String,
    val type: String,
    val category: String,
    val reporter_name: String,
    val date_reported: String,
    val status: String, // "ACTIVE", "RESOLVED"
    val contact_info: String
)
