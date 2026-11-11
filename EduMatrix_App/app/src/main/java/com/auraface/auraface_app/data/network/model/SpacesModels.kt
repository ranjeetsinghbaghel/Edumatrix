package com.auraface.auraface_app.data.network.model

data class SpaceItem(
    val id: String,
    val name: String,
    val type: String,
    val capacity: Int,
    val is_available: Boolean,
    val next_available_time: String?,
    val icon_emoji: String,
    val location: String
)

data class BookingRequest(
    val space_id: String,
    val date: String,
    val time_slot: String,
    val purpose: String
)

data class BookingResponse(
    val booking_id: String,
    val space_name: String,
    val date: String,
    val time_slot: String,
    val status: String,
    val message: String
)

data class MyBookingOut(
    val id: String,
    val space_name: String,
    val type: String,
    val date: String,
    val time_slot: String,
    val status: String,
    val icon_emoji: String
)
