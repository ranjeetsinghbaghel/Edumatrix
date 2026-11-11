package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.domain.model.Student
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

@JvmSuppressWildcards
interface StudentApi {

    @GET("admin/students/")
    suspend fun getStudents(): List<Student>

    @Multipart
    @POST("admin/students/")
    suspend fun addStudent(
        @Part("name") name: RequestBody,
        @Part("roll_no") rollNo: RequestBody,
        @Part("program") program: RequestBody?,
        @Part("department") department: RequestBody,
        @Part("year") year: RequestBody,
        @Part("semester") semester: RequestBody?,
        @Part("section") section: RequestBody,
        @Part("email") email: RequestBody?,
        @Part("mobile") mobile: RequestBody?,
        @Part("guardian_name") guardianName: RequestBody?,
        @Part("guardian_email") guardianEmail: RequestBody?,
        @Part("guardian_mobile") guardianMobile: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Student

    @Multipart
    @PUT("admin/students/{id}")
    suspend fun updateStudent(
        @Path("id") id: Int,
        @Part("name") name: RequestBody?,
        @Part("roll_no") rollNo: RequestBody?,
        @Part("program") program: RequestBody?,
        @Part("department") department: RequestBody?,
        @Part("year") year: RequestBody?,
        @Part("semester") semester: RequestBody?,
        @Part("section") section: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part("mobile") mobile: RequestBody?,
        @Part("guardian_name") guardianName: RequestBody?,
        @Part("guardian_email") guardianEmail: RequestBody?,
        @Part("guardian_mobile") guardianMobile: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Student

    @DELETE("admin/students/{id}")
    suspend fun deleteStudent(@Path("id") id: Int)

    // Student specific
    @GET("student/profile")
    suspend fun getProfile(): Student

    @GET("student/attendance")
    suspend fun getMyAttendance(): List<AttendanceDTO>

    @POST("student/leave/apply")
    suspend fun submitLeave(@Body body: Map<String, String>): Map<String, String>

    @PUT("student/profile/blood-group")
    suspend fun updateBloodGroup(@Body body: Map<String, String>): Map<String, String>

    @GET("student/leave/my-leaves")
    suspend fun getMyLeaves(): List<LeaveDTO>

    @POST("student/change-password")
    suspend fun changePassword(@Body body: Map<String, String>): Map<String, String>

    @GET("student/timetable")
    suspend fun getTimetable(): List<TimetableDTO>

    @GET("student/next-class")
    suspend fun getNextClass(): TimetableDTO?

    // --- New Academic Features ---
    @GET("academic/exams")
    suspend fun getExamSchedule(): List<ExamScheduleDTO>

    @GET("academic/results/me")
    suspend fun getMyResults(): List<ResultDTO>

    @GET("academic/cgpa/me")
    suspend fun getMyCGPA(): GpaDTO

    @GET("notifications/")
    suspend fun getNotifications(): List<com.auraface.auraface_app.data.network.model.Notification>

    @GET("student/proctor/meetings")
    suspend fun getMyProctorMeetings(): List<com.auraface.auraface_app.data.network.model.ProctorMeeting>

    @POST("student/message/class-teacher")
    suspend fun sendMessageToClassTeacher(@Body body: Map<String, String>): Map<String, String>

    @PUT("student/proctor/meeting/{id}/respond")
    suspend fun respondToProctorMeeting(@Path("id") id: Int, @Body body: Map<String, String>): Map<String, String>

    @GET("student/teachers/availability")
    suspend fun getTeacherAvailability(): List<TeacherAvailabilityDTO>

    @GET("student/calendar")
    suspend fun getAcademicCalendar(): List<CalendarEventDTO>
    
    // --- Entertainment ---
    
    @GET("movies/")
    suspend fun getMovies(): List<com.auraface.auraface_app.data.remote.dto.MovieEventDto>
    
    @POST("movies/book")
    suspend fun bookMovie(@Body req: Map<String, Int>): com.auraface.auraface_app.data.remote.dto.MovieBookingDto
    
    @GET("movies/my-bookings")
    suspend fun getMyMovieBookings(): List<com.auraface.auraface_app.data.remote.dto.MovieBookingDto>
    
    @GET("sports/tournaments")
    suspend fun getSportsTournaments(): List<com.auraface.auraface_app.data.remote.dto.SportsTournamentDto>
    
    @GET("sports/live-matches")
    suspend fun getLiveSportsMatches(): List<com.auraface.auraface_app.data.remote.dto.SportsMatchDto>
    
    @GET("sports/tournaments/{tournament_id}/matches")
    suspend fun getMatchesForTournament(@Path("tournament_id") tournamentId: Int): List<com.auraface.auraface_app.data.remote.dto.SportsMatchDto>

    @GET("fees/dues")
    suspend fun getFeeDues(): com.auraface.auraface_app.data.remote.dto.FeeDuesDto

    @POST("fees/pay")
    suspend fun payFee(@Body body: com.auraface.auraface_app.data.remote.dto.FeePayRequestDto): com.auraface.auraface_app.data.remote.dto.FeePaymentResponseDto

    @GET("fees/transactions")
    suspend fun getFeeTransactions(): List<com.auraface.auraface_app.data.remote.dto.FeeTransactionDto>

    @POST("fees/request-parent")
    suspend fun requestParentPayment(@Body body: com.auraface.auraface_app.data.remote.dto.ParentPaymentRequestDto): com.auraface.auraface_app.data.remote.dto.ParentPaymentResponseDto
}

data class AttendanceDTO(
    val id: Int,
    val subject: String?,
    val date: String?,
    val status: String?,
    val period: String?
)

data class TimetableDTO(
    val id: Int,
    val day: String?,
    val time: String?,
    val subject: String?,
    val room: String?,
    val period: String? = null,
    val date: String? = null
)

data class LeaveDTO(
    val id: Int,
    val reason: String?,
    val date: String?,
    val status: String?
)

data class ExamScheduleDTO(
    val id: Int,
    val subject_name: String?,
    val exam_type: String?,
    val date: String?,
    val start_time: String?,
    val end_time: String?,
    val room: String?
)

data class ResultDTO(
    val id: Int,
    val subject_id: String,
    val subject_name: String? = null,
    val assessment_type: String, // Midterm, Final
    val score: Float,
    val total_marks: Float
)

data class GpaDTO(
    val student_id: Int,
    val student_name: String?,
    val total_credits: Float,
    val total_points: Float,
    val cgpa: Float
)

data class CalendarEventDTO(
    val id: Int? = null,
    val title: String,
    val date: String,
    val type: String
)

data class TeacherAvailabilityDTO(
    val id: String,
    val name: String,
    val department: String,
    val status: String
)
