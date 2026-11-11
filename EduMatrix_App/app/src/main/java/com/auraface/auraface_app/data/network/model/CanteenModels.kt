package com.auraface.auraface_app.data.network.model

data class MenuItem(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val calories: Int,
    val is_veg: Boolean,
    val rating: Double,
    val prep_time_mins: Int,
    val image_emoji: String
)

data class CanteenStatus(
    val is_open: Boolean,
    val crowd_percentage: Int,
    val wait_time_mins: Int,
    val popular_now: String
)

data class OrderItem(
    val menu_id: String,
    val quantity: Int
)

data class OrderRequest(
    val items: List<OrderItem>,
    val pickup_time: String,
    val notes: String = ""
)

data class OrderResponse(
    val order_id: String,
    val status: String,
    val total_amount: Double,
    val estimated_ready_time: String,
    val message: String
)
