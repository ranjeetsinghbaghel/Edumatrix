package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.SubjectApi
import com.auraface.auraface_app.domain.model.Subject
import javax.inject.Inject

class SubjectRepository @Inject constructor(
    private val api: SubjectApi
) {
    suspend fun getSubjects() = api.getSubjects()
    suspend fun addSubject(subject: Subject) = api.addSubject(subject)
    suspend fun updateSubject(subject: Subject) = api.updateSubject(subject.id, subject)
    suspend fun deleteSubject(id: String) = api.deleteSubject(id)
}
