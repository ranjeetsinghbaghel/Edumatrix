package com.auraface.auraface_app.data.remote.dto

data class NoticeDto(
    val id: Int,
    val title: String,
    val message: String,
    val priority: String,
    val target_audience: String,
    val is_read: Boolean,
    val view_count: Int,
    val created_at: String,
    val image_url: String?,
    val expiry_date: String?
)

data class ChatResponseDto(
    val reply: String,
    val action: String?
)

data class TrendPointDto(
    val date: String,
    val percentage: Double
)

data class StudentRiskDto(
    val risk_level: String, // HIGH, MEDIUM, LOW
    val message: String,
    val missable_classes: Int,
    val current_percentage: Double
)

data class RequiredClassesDto(
    val target_percentage: Double,
    val required_classes: Int
)

data class ChatRequestDto(
    val message: String
)

data class NoticeCreateDto(
    val title: String,
    val message: String,
    val target_audience: String = "ALL", // ALL, STUDENTS, TEACHERS
    val priority: String = "LOW", // HIGH, MEDIUM, LOW
    val image_url: String? = null,
    val expiry_date: String? = null,
    val department: String? = null,
    val year: Int? = null,
    val section: String? = null,
    val scheduled_at: String? = null
)

data class NoticeReaderDto(
    val user_id: Int,
    val name: String,
    val role: String,
    val read_at: String
)

data class MovieEventDto(
    val id: Int,
    val title: String,
    val description: String?,
    val poster_url: String?,
    val trailer_url: String?,
    val show_time: String,
    val venue: String,
    val duration_mins: Int?,
    val total_seats: Int?,
    val available_seats: Int?,
    val ticket_price: Double?,
    val status: String?,
    val created_at: String?
)

data class MovieBookingDto(
    val id: Int,
    val movie_id: Int,
    val student_id: Int,
    val seats_booked: Int?,
    val booking_time: String?,
    val status: String?
)

data class SportsTournamentDto(
    val id: Int,
    val name: String,
    val sport_type: String,
    val start_date: String,
    val end_date: String,
    val registration_deadline: String?,
    val status: String?,
    val banner_url: String?,
    val created_at: String?
)

data class SportsMatchDto(
    val id: Int,
    val tournament_id: Int,
    val team_a: String,
    val team_b: String,
    val match_time: String,
    val venue: String,
    val status: String,
    val score_team_a: String?,
    val score_team_b: String?,
    val winner: String?,
    val is_live: Boolean?
)
