package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.domain.model.Teacher
import retrofit2.http.*

@JvmSuppressWildcards
interface TeacherApi {

    @GET("admin/teachers/")
    suspend fun getTeachers(): List<Teacher>

    @Multipart
    @POST("admin/teachers/")
    suspend fun addTeacher(
        @Part("username") username: okhttp3.RequestBody,
        @Part("full_name") fullName: okhttp3.RequestBody,
        @Part("password") password: okhttp3.RequestBody,
        @Part("email") email: okhttp3.RequestBody?,
        @Part("mobile") mobile: okhttp3.RequestBody?,
        @Part("address") address: okhttp3.RequestBody?,
        @Part("qualification") qualification: okhttp3.RequestBody?,
        @Part file: okhttp3.MultipartBody.Part?
    ): Teacher

    @Multipart
    @PUT("admin/teachers/{id}")
    suspend fun updateTeacher(
        @Path("id") id: Int,
        @Part("username") username: okhttp3.RequestBody?,
        @Part("full_name") fullName: okhttp3.RequestBody?,
        @Part("password") password: okhttp3.RequestBody?,
        @Part("email") email: okhttp3.RequestBody?,
        @Part("mobile") mobile: okhttp3.RequestBody?,
        @Part("address") address: okhttp3.RequestBody?,
        @Part("qualification") qualification: okhttp3.RequestBody?,
        @Part file: okhttp3.MultipartBody.Part?
    ): Teacher

    @DELETE("admin/teachers/{id}")
    suspend fun deleteTeacher(@Path("id") id: Int)

    @POST("admin/teachers/assign-hod")
    suspend fun assignHod(@Query("teacher_id") teacherId: Int, @Query("department") department: String): Map<String, String>

    @POST("admin/teachers/assign-class-teacher")
    suspend fun assignClassTeacher(
        @Query("teacher_id") teacherId: Int,
        @Query("department") department: String,
        @Query("year") year: Int,
        @Query("semester") semester: Int,
        @Query("section") section: String
    ): Map<String, String>

    @GET("teacher/dashboard-data")
    suspend fun getDashboardData(): com.auraface.auraface_app.data.network.model.TeacherAnalytics

    @PUT("teacher/availability")
    suspend fun updateAvailability(@Body req: Map<String, String>): Map<String, String>

    @PUT("teacher/availability-toggle")
    suspend fun updateAvailabilityToggle(@Body req: Map<String, Boolean>): Map<String, String>

    @GET("teacher/leave/assigned-section-requests")
    suspend fun getSectionLeaves(): List<Map<String, Any>>

    @POST("teacher/leave/section-action")
    suspend fun sectionAction(@Body body: com.auraface.auraface_app.data.network.model.LeaveActionRequest): Map<String, String>

    @GET("teacher/leave/hod-requests")
    suspend fun getHodLeaves(): List<Map<String, Any>>

    @POST("teacher/leave/hod-action")
    suspend fun hodAction(@Body body: com.auraface.auraface_app.data.network.model.HodActionRequest): Map<String, String>

    @GET("attendance/subject/{subject_id}")
    suspend fun getSubjectHistory(@Path("subject_id") subjectId: String): com.auraface.auraface_app.data.network.model.ApiResponse<List<com.auraface.auraface_app.data.network.model.AttendanceHistoryRecord>>

    @POST("teacher/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Map<String, String>

    @GET("teacher/timetable")
    suspend fun getTimetable(): List<com.auraface.auraface_app.data.network.model.TimetableItem>

    @POST("teacher/schedule/makeup")
    suspend fun createMakeupClass(@Body req: com.auraface.auraface_app.data.network.model.MakeupClassCreate): com.auraface.auraface_app.data.network.model.ApiResponse<Map<String, Any>>

    @POST("academic/marks")
    suspend fun addMarks(@Body body: com.auraface.auraface_app.data.network.model.MarksCreateRequest): com.auraface.auraface_app.data.network.model.MarksResponse

    @GET("academic/marks/subject/{subject_id}")
    suspend fun getSubjectMarks(@Path("subject_id") subjectId: String): List<com.auraface.auraface_app.data.network.model.MarksResponse>

    @POST("academic/marks/bulk")
    suspend fun addMarksBulk(@Body body: com.auraface.auraface_app.data.network.model.MarksBulkCreate): com.auraface.auraface_app.data.network.model.ApiResponse<Map<String, String>>

    @GET("teacher/attendance/students")
    suspend fun getStudentsForClass(
        @Query("department") department: String,
        @Query("year") year: Int,
        @Query("section") section: String
    ): List<com.auraface.auraface_app.data.network.model.StudentBasic>

    @GET("academic/subjects")
    suspend fun getSubjects(
        @Query("department") department: String?,
        @Query("year") year: Int?,
        @Query("section") section: String?
    ): List<com.auraface.auraface_app.data.network.model.SubjectDTO>
    
    @GET("teacher/proctor/meetings")
    suspend fun getProctorMeetings(@Query("student_id") studentId: Int?): List<com.auraface.auraface_app.data.network.model.ProctorMeeting>

    @POST("teacher/proctor/meeting")
    suspend fun addProctorMeeting(@Body meeting: com.auraface.auraface_app.data.network.model.ProctorMeetingCreate): com.auraface.auraface_app.data.network.model.ProctorMeeting

    @GET("academic/results/student/{studentId}")
    suspend fun getStudentResults(@Path("studentId") studentId: Int): List<com.auraface.auraface_app.data.network.model.MarksResponse>

    @POST("teacher/section/message")
    suspend fun sendSectionMessage(@Body body: com.auraface.auraface_app.data.network.model.SectionMessageCreate): retrofit2.Response<Unit>

    @GET("notifications/")
    suspend fun getNotifications(): List<com.auraface.auraface_app.data.network.model.Notification>

    @GET("teacher/sent-messages")
    suspend fun getSentMessages(): List<com.auraface.auraface_app.data.network.model.SentMessage>

    @GET("teacher/students/search")
    suspend fun searchStudents(
        @Query("year") year: Int?,
        @Query("department") department: String?,
        @Query("semester") semester: Int?,
        @Query("section") section: String?,
        @Query("query") query: String?
    ): List<TeacherStudentSearchItem>

    @GET("teacher/students/{id}/profile")
    suspend fun getStudentProfileDetail(@Path("id") id: Int): TeacherStudentProfileDetail
}

data class TeacherStudentSearchItem(
    val id: Int,
    val name: String,
    val roll_no: String,
    val department: String? = null,
    val year: Int? = null,
    val semester: Int? = null,
    val section: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val profile_image: String? = null,
    val guardian_name: String? = null,
    val guardian_mobile: String? = null,
    val guardian_email: String? = null,
    val blood_group: String? = null,
    val attendance_percentage: Float? = null,
    val total_classes: Int? = null,
    val attended_classes: Int? = null
)

data class TeacherStudentProfileDetail(
    val id: Int,
    val name: String,
    val roll_no: String,
    val department: String? = null,
    val year: Int? = null,
    val semester: Int? = null,
    val section: String? = null,
    val program: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    val blood_group: String? = null,
    val profile_image: String? = null,
    val guardian_name: String? = null,
    val guardian_mobile: String? = null,
    val guardian_email: String? = null,
    val attendance: com.auraface.auraface_app.data.network.api.ErpAttendanceReport? = null,
    val marks: List<com.auraface.auraface_app.data.network.api.ErpSubjectMarks> = emptyList(),
    val erp_report: com.auraface.auraface_app.data.network.api.ErpFullReport? = null
)

