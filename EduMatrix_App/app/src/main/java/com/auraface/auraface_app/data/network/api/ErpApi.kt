package com.auraface.auraface_app.data.network.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class ErpCaptchaResponse(
    val session_id: String,
    val captcha_image_base64: String
)

data class ErpSyncRequest(
    val session_id: String,
    val username: String,
    val password: String,
    val captcha_code: String
)

data class ErpSubjectAttendance(
    val code: String? = null,
    val name: String? = null,
    val total_classes: String? = null,
    val attended: String? = null,
    val percentage: String? = null
)

data class ErpAttendanceReport(
    val overall_percentage: String? = "0%",
    val total_classes_held: Int? = 0,
    val total_classes_attended: Int? = 0,
    val status: String? = null,
    val subjects: List<ErpSubjectAttendance> = emptyList()
)

data class ErpSubjectMarks(
    val code: String? = null,
    val subject: String? = null,
    val internal_1: String? = "-",
    val internal_2: String? = "-",
    val assignment: String? = "-",
    val lab_marks: String? = "-",
    val total: String? = "-",
    val grade: String? = "-"
)

data class ErpMarksReport(
    val cgpa: String? = "N/A",
    val sgpa_previous: String? = "N/A",
    val academic_standing: String? = null,
    val subjects: List<ErpSubjectMarks> = emptyList()
)

data class ErpFeeItem(
    val head: String? = null,
    val total: String? = null,
    val paid: String? = null,
    val due: String? = null,
    val status: String? = null
)

data class ErpFeesReport(
    val total_outstanding: String? = "₹0.00",
    val dues: List<ErpFeeItem> = emptyList()
)

data class ErpProfile(
    val roll_no: String? = null,
    val full_name: String? = null,
    val program: String? = "B.Tech",
    val department: String? = "CSE",
    val semester: Int? = 7,
    val section: String? = "G",
    val year: String? = "4th Year",
    val batch: String? = "2023 - 2027",
    val proctor: String? = null,
    val blood_group: String? = null
)

data class ErpFullReport(
    val roll_no: String? = null,
    val last_synced_at: String? = null,
    val university: String? = "GIET University, Gunupur",
    val portal: String? = "https://gietuerp.in",
    val profile: ErpProfile? = null,
    val attendance: ErpAttendanceReport? = null,
    val marks: ErpMarksReport? = null,
    val fees: ErpFeesReport? = null
)

data class ErpSyncResponse(
    val success: Boolean,
    val message: String,
    val data: ErpFullReport? = null
)

data class ErpStatusResponse(
    val status: String,
    val portal: String,
    val message: String? = null,
    val error: String? = null
)

interface ErpApi {
    @GET("erp/status")
    suspend fun getStatus(): ErpStatusResponse

    @GET("erp/captcha")
    suspend fun getCaptcha(): ErpCaptchaResponse

    @GET("erp/student/{roll_no}/data")
    suspend fun getStudentErpData(@Path("roll_no") rollNo: String): ErpFullReport

    @POST("erp/sync")
    suspend fun syncData(@Body request: ErpSyncRequest): ErpSyncResponse
}
