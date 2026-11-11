package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.network.model.AttendanceRequest
import com.auraface.auraface_app.data.network.model.AttendanceResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AttendanceApi {

    @POST("/attendance/mark")
    suspend fun markAttendance(
        @Body request: AttendanceRequest
    ): com.auraface.auraface_app.data.network.model.ApiResponse<AttendanceResponse>

    @POST("/attendance/mark-manual")
    suspend fun markAttendanceManual(
        @Body request: com.auraface.auraface_app.data.network.model.ManualAttendanceRequest
    ): com.auraface.auraface_app.data.network.model.ApiResponse<AttendanceResponse>

    @GET("attendance/student")
    suspend fun getAttendance(): List<Any>

    @GET("teacher/attendance/students")
    suspend fun getStudentsByClass(
        @retrofit2.http.Query("year") year: Int,
        @retrofit2.http.Query("section") section: String
    ): List<com.auraface.auraface_app.data.network.model.AttendanceStudent>

    @POST("teacher/attendance/mark-bulk")
    suspend fun markBulkAttendance(
        @Body request: com.auraface.auraface_app.data.network.model.BulkAttendanceMark
    ): Map<String, String>
}


