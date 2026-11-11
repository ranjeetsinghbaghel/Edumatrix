package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.data.remote.dto.*
import retrofit2.http.*

@JvmSuppressWildcards
interface PlacementApi {

    @GET("placement/profile")
    suspend fun getReadinessProfile(): StudentReadinessDto

    @POST("placement/skill")
    suspend fun addSkill(@Body skill: Map<String, String>): PlacementSkillDto

    @POST("placement/certification")
    suspend fun addCertification(@Body cert: Map<String, String>): PlacementCertificationDto

    @POST("placement/internship")
    suspend fun addInternship(@Body intern: Map<String, Any>): PlacementInternshipDto

    @POST("placement/project")
    suspend fun addProject(@Body project: Map<String, String>): PlacementProjectDto

    @POST("placement/event")
    suspend fun addEvent(@Body event: Map<String, String>): PlacementEventDto
    
    @Multipart
    @POST("placement/upload")
    suspend fun uploadDocument(
        @Part file: okhttp3.MultipartBody.Part
    ): ChatUploadResponse // Reusing this DTO since it has url/filename

    @DELETE("placement/skill/{id}")
    suspend fun deleteSkill(@Path("id") id: Int)

    @DELETE("placement/certification/{id}")
    suspend fun deleteCertification(@Path("id") id: Int)

    @DELETE("placement/internship/{id}")
    suspend fun deleteInternship(@Path("id") id: Int)

    @DELETE("placement/project/{id}")
    suspend fun deleteProject(@Path("id") id: Int)

    @DELETE("placement/event/{id}")
    suspend fun deleteEvent(@Path("id") id: Int)

    @PUT("placement/skill/{id}")
    suspend fun editSkill(@Path("id") id: Int, @Body skill: Map<String, String>): PlacementSkillDto

    @PUT("placement/certification/{id}")
    suspend fun editCertification(@Path("id") id: Int, @Body cert: Map<String, String>): PlacementCertificationDto

    @PUT("placement/internship/{id}")
    suspend fun editInternship(@Path("id") id: Int, @Body intern: Map<String, Any>): PlacementInternshipDto

    @PUT("placement/project/{id}")
    suspend fun editProject(@Path("id") id: Int, @Body project: Map<String, String>): PlacementProjectDto

    @PUT("placement/event/{id}")
    suspend fun editEvent(@Path("id") id: Int, @Body event: Map<String, String>): PlacementEventDto

    @POST("placement/interview/start")
    suspend fun startInterview(@Body req: Map<String, String>): List<String>

    @POST("placement/interview/submit")
    suspend fun submitInterview(@Body body: Map<String, Any>): AIInterviewResultDto

    @GET("placement/resume")
    suspend fun getResume(): ResumeProfileDto

    @POST("placement/resume/save")
    suspend fun saveResume(@Body profile: ResumeProfileDto): Map<String, String>
}
