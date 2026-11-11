package com.auraface.auraface_app.data.network.api

import com.auraface.auraface_app.domain.model.Subject
import retrofit2.http.*

interface SubjectApi {

    @GET("admin/subjects/")
    suspend fun getSubjects(): List<Subject>

    @POST("admin/subjects/")
    suspend fun addSubject(@Body subject: Subject): Subject

    @PUT("admin/subjects/{id}")
    suspend fun updateSubject(
        @Path("id") id: String,
        @Body subject: Subject
    ): Subject

    @DELETE("admin/subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: String)
}
