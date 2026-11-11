package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.PlacementApi
import com.auraface.auraface_app.data.remote.dto.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacementRepository @Inject constructor(
    private val api: PlacementApi
) {
    suspend fun getReadinessProfile() = api.getReadinessProfile()

    suspend fun addSkill(name: String, proficiency: String, documentUrl: String? = null) = 
        api.addSkill(mapOf("skill_name" to name, "proficiency" to proficiency, "document_url" to (documentUrl ?: "")))
    
    suspend fun addCertification(name: String, issuer: String, credentialUrl: String? = null) =
        api.addCertification(
            mapOf(
                "name" to name, 
                "issuing_org" to issuer, 
                "credential_url" to (credentialUrl ?: "")
            )
        )

    suspend fun addProject(title: String, description: String, techStack: String, status: String, documentUrl: String? = null) =
        api.addProject(mapOf("title" to title, "description" to description, "tech_stack" to techStack, "project_status" to status, "document_url" to (documentUrl ?: "")))
    
    suspend fun addInternship(company: String, role: String, startDate: String, endDate: String, documentUrl: String? = null) =
        api.addInternship(
            mutableMapOf<String, Any>("company_name" to company, "role" to role, "start_date" to startDate).apply {
                if (endDate.isNotBlank()) put("end_date", endDate)
                if (documentUrl != null && documentUrl.isNotBlank()) put("certificate_url", documentUrl)
            }
        )
        
    suspend fun addEvent(name: String, type: String, date: String, documentUrl: String? = null) =
        api.addEvent(mapOf("event_name" to name, "event_type" to type, "date" to date, "document_url" to (documentUrl ?: "")))

    suspend fun uploadDocument(file: java.io.File): String? {
        return try {
             val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
             val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
             api.uploadDocument(body).url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteSkill(id: Int) = api.deleteSkill(id)
    suspend fun deleteCertification(id: Int) = api.deleteCertification(id)
    suspend fun deleteInternship(id: Int) = api.deleteInternship(id)
    suspend fun deleteProject(id: Int) = api.deleteProject(id)
    suspend fun deleteEvent(id: Int) = api.deleteEvent(id)

    suspend fun editSkill(id: Int, name: String, proficiency: String, documentUrl: String? = null) = 
        api.editSkill(id, mapOf("skill_name" to name, "proficiency" to proficiency, "document_url" to (documentUrl ?: "")))
    
    suspend fun editCertification(id: Int, name: String, issuer: String, credentialUrl: String? = null) =
        api.editCertification(id, mapOf("name" to name, "issuing_org" to issuer, "credential_url" to (credentialUrl ?: "")))

    suspend fun editProject(id: Int, title: String, description: String, techStack: String, status: String, documentUrl: String? = null) =
        api.editProject(id, mapOf("title" to title, "description" to description, "tech_stack" to techStack, "project_status" to status, "document_url" to (documentUrl ?: "")))
    
    suspend fun editInternship(id: Int, company: String, role: String, startDate: String, endDate: String, documentUrl: String? = null): PlacementInternshipDto {
        val map = mutableMapOf<String, Any>("company_name" to company, "role" to role, "start_date" to startDate)
        if (endDate.isNotBlank()) map["end_date"] = endDate
        if (documentUrl != null) map["certificate_url"] = documentUrl
        return api.editInternship(id, map)
    }
        
    suspend fun editEvent(id: Int, name: String, type: String, date: String, documentUrl: String? = null) =
        api.editEvent(id, mapOf("event_name" to name, "event_type" to type, "date" to date, "document_url" to (documentUrl ?: "")))
}
