package com.auraface.auraface_app.data.network.model

data class MarksCreateRequest(
    val student_id: Int? = null,
    val roll_no: String? = null,
    val subject_id: String,
    val assessment_type: String,
    val score: Float,
    val total_marks: Float
)

data class MarksResponse(
    val id: Int,
    val student_id: Int,
    val subject_id: String,
    val assessment_type: String,
    val score: Float,
    val total_marks: Float,
    val student_name: String? = null,
    val roll_no: String? = null,
    val subject_name: String? = null
)

data class QuickMarkCreate(
    val student_id: Int,
    val score: Float
)

data class MarksBulkCreate(
    val subject_id: String,
    val assessment_type: String,
    val total_marks: Float,
    val marks: List<QuickMarkCreate>
)

data class StudentBasic(
    val id: Int,
    val name: String,
    val roll_no: String?,
    var currentScore: String = "",
    val guardian_name: String? = null,
    val guardian_mobile: String? = null,
    val guardian_email: String? = null
)

data class SubjectDTO(
    val id: String,
    val name: String,
    val department: String?,
    val year: Int?,
    val semester: Int?,
    val section: String?,
    val credits: Int
)

data class ProctorMeeting(
    val id: Int,
    val teacher_id: Int,
    val student_id: Int,
    val student_name: String?,
    val date: String,
    val remarks: String,
    val action_taken: String?,
    val student_response: String? = null
)

data class ProctorMeetingCreate(
    val student_id: Int,
    val date: String,
    val remarks: String,
    val action_taken: String?
)

data class SectionMessageCreate(
    val message: String,
    val student_id: Int? = null,
    val department: String? = null,
    val year: Int? = null,
    val section: String? = null
)

data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val is_read: Boolean,
    val created_at: String,
    val metadata_json: Map<String, Any>? = null
)

data class SentMessage(
    val id: Int,
    val content: String,
    val target_group: String?,
    val created_at: String,
    val read_count: Int,
    val total_count: Int
)
