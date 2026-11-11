package com.auraface.auraface_app.data.network.model

data class AssignedSubject(
    val id: String,
    val name: String,
    val department: String? = null,
    val year: Int? = null,
    val semester: Int? = null,
    val section: String? = null,
    val studentCount: Int,
    val lastAttendance: String?
)

data class TeacherAnalytics(
    val totalAssignedSubjects: Int,
    val pendingAttendanceToday: Int,
    val assignedSubjects: List<AssignedSubject>,
    val isHod: Boolean = false,
    val hodDepartment: String? = null,
    val isClassTeacher: Boolean = false,
    val myClass: ClassDetails? = null,
    val customAvailabilityMessage: String? = null,
    val isAvailable: Boolean = true
)

data class ClassDetails(
    val department: String,
    val year: Int,
    val section: String
)
