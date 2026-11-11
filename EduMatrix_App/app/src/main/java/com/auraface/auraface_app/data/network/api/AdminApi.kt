package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.ApiResponse
import com.auraface.auraface_app.data.network.model.TimetableItem
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

@JvmSuppressWildcards
interface AdminApi {
    @GET("admin/timetable/list")
    suspend fun getTimetable(
        @Query("department") department: String,
        @Query("year") year: Int,
        @Query("section") section: String,
        @Query("semester") semester: Int? = null
    ): ApiResponse<List<TimetableItem>>

    @GET("admin/dashboard-data")
    suspend fun getDashboardAnalytics(): com.auraface.auraface_app.data.network.model.DashboardAnalytics

    @GET("admin/charts/defaulters")
    suspend fun getDefaulters(): com.auraface.auraface_app.data.network.model.DefaulterCount

    @POST("admin/charts/defaulters/notify")
    suspend fun notifyDefaulters(@Body req: Map<String, List<Int>>): Map<String, Any>

    @GET("admin/charts/most-absent")
    suspend fun getMostAbsentSubjects(): List<com.auraface.auraface_app.data.network.model.SubjectAbsence>

    @POST("admin/timetable/add")
    suspend fun addTimetableSlot(@Body slot: com.auraface.auraface_app.data.network.model.TimetableSlotCreate): ApiResponse<Map<String, Any>>

    @DELETE("admin/timetable/delete/{id}")
    suspend fun deleteTimetableSlot(@Path("id") id: Int): ApiResponse<Any>

    @PUT("admin/timetable/update/{id}")
    suspend fun updateTimetableSlot(@Path("id") id: Int, @Body slot: com.auraface.auraface_app.data.network.model.TimetableSlotCreate): ApiResponse<Any>

    @POST("notifications/send-notice")
    suspend fun sendNotice(@Body notice: com.auraface.auraface_app.data.remote.dto.NoticeCreateDto): Map<String, Any>

    @POST("admin/calendar")
    suspend fun addCalendarEvent(@Body event: Map<String, String>): com.auraface.auraface_app.data.network.api.CalendarEventDTO

    @DELETE("admin/calendar/{id}")
    suspend fun deleteCalendarEvent(@Path("id") id: Int): Map<String, String>

    @GET("student/calendar")
    suspend fun getAcademicCalendar(): List<com.auraface.auraface_app.data.network.api.CalendarEventDTO>
}
