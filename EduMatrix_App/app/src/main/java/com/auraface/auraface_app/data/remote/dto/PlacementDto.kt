package com.auraface.auraface_app.data.remote.dto

data class PlacementProfileDto(
    val id: Int,
    val student_id: Int,
    val linkedin_link: String?,
    val github_link: String?,
    val portfolio_link: String?,
    val resume_url: String?,
    val resume_version: Int,
    val readiness_score: Float,
    val updated_at: String
)

data class PlacementSkillDto(
    val id: Int,
    val skill_name: String,
    val proficiency: String,
    val is_verified: Boolean
)

data class PlacementCertificationDto(
    val id: Int,
    val name: String,
    val issuing_org: String,
    val issue_date: String?,
    val credential_url: String?,
    val verification_status: String
)

data class PlacementInternshipDto(
    val id: Int,
    val company_name: String,
    val role: String,
    val start_date: String,
    val end_date: String?,
    val is_current: Boolean,
    val verification_status: String
)

data class PlacementProjectDto(
    val id: Int,
    val title: String,
    val description: String,
    val tech_stack: String,
    val approval_status: String,
    val project_status: String = "ONGOING",
    val rejection_reason: String?
)

data class PlacementEventDto(
    val id: Int,
    val event_name: String,
    val event_type: String,
    val date: String,
    val achievement: String?,
    val verification_status: String
)

data class StudentReadinessDto(
    val profile: PlacementProfileDto?,
    val skills: List<PlacementSkillDto>,
    val certifications: List<PlacementCertificationDto>,
    val internships: List<PlacementInternshipDto>,
    val projects: List<PlacementProjectDto>,
    val events: List<PlacementEventDto>,
    val total_score: Float,
    val missing_elements: List<String>
)
