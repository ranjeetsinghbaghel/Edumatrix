package com.auraface.auraface_app.data.remote.dto

data class AIInterviewResultDto(
    val score: Int,
    val overall_feedback: String,
    val feedback_by_question: List<String>,
    val strengths: List<String>,
    val weaknesses: List<String>
)

data class ResumeProfileDto(
    val full_name: String?,
    val email: String?,
    val phone: String?,
    val linkedin: String?,
    val github: String?,
    val education: String?,
    val experience: List<Map<String, Any>> = emptyList(),
    val projects: List<Map<String, Any>> = emptyList(),
    val skills: List<String> = emptyList()
)

data class MockTestResultDto(
    val score: Int,
    val total_questions: Int,
    val xp_earned: Int,
    val time_taken_seconds: Int,
    val correct_answers: Map<String, Int>, // JSON maps keys to Strings in Retrofit
    val explanations: Map<String, String>
)

data class LibrarySeatDto(
    val id: Int,
    val seat_no: String,
    val floor: Int,
    val zone: String,
    val is_booked: Boolean,
    val booking_time_slot: String?
)

data class LibraryBookingResponseDto(
    val seat_no: String,
    val floor: Int,
    val zone: String,
    val time_slot: String,
    val ticket_no: String,
    val message: String
)

data class FeeDuesDto(
    val tuition_fee_due: Float,
    val hostel_fee_due: Float,
    val exam_fee_due: Float,
    val library_fine_due: Float,
    val total_due: Float,
    val parent_name: String,
    val parent_phone: String
)

data class FeePaymentResponseDto(
    val transaction_id: String,
    val amount: Float,
    val category: String,
    val payment_method: String,
    val status: String,
    val paid_at: String,
    val message: String
)

data class FeeTransactionDto(
    val transaction_id: String,
    val amount: Float,
    val category: String,
    val payment_method: String,
    val status: String,
    val paid_at: String
)

data class FeePayRequestDto(
    val category: String,
    val amount: Float,
    val payment_method: String = "UPI"
)

data class ParentPaymentRequestDto(
    val category: String,
    val amount: Float,
    val parent_name: String,
    val parent_phone: String
)

data class ParentPaymentResponseDto(
    val message: String
)
