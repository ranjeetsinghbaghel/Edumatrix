package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.TeacherApi
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class TeacherRepository @Inject constructor(
    private val api: TeacherApi
) {
    suspend fun getTeachers() = api.getTeachers()
    private fun createPart(value: String): okhttp3.RequestBody {
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    suspend fun addTeacher(teacher: com.auraface.auraface_app.domain.model.Teacher, imageFile: File?) = api.addTeacher(
        username = createPart(teacher.username),
        fullName = createPart(teacher.full_name ?: ""),
        password = createPart(teacher.password ?: ""),
        email = teacher.email?.let { createPart(it) },
        mobile = teacher.mobile?.let { createPart(it) },
        address = teacher.address?.let { createPart(it) },
        qualification = teacher.qualification?.let { createPart(it) },
        file = imageFile?.let {
            val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("file", it.name, requestFile)
        }
    )

    suspend fun updateTeacher(teacher: com.auraface.auraface_app.domain.model.Teacher, imageFile: File?) = api.updateTeacher(
        id = teacher.id,
        username = createPart(teacher.username),
        fullName = teacher.full_name?.let { createPart(it) },
        password = teacher.password?.let { createPart(it) },
        email = teacher.email?.let { createPart(it) },
        mobile = teacher.mobile?.let { createPart(it) },
        address = teacher.address?.let { createPart(it) },
        qualification = teacher.qualification?.let { createPart(it) },
        file = imageFile?.let {
             val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
             MultipartBody.Part.createFormData("file", it.name, requestFile)
        }
    )
    suspend fun deleteTeacher(id: Int) = api.deleteTeacher(id)

    suspend fun getTeacherDashboardData() = api.getDashboardData()

    suspend fun updateAvailability(message: String) = api.updateAvailability(mapOf("message" to message))
    suspend fun updateAvailabilityToggle(isAvailable: Boolean) = api.updateAvailabilityToggle(mapOf("is_available" to isAvailable))

    suspend fun getSectionLeaves() = api.getSectionLeaves()
    suspend fun sectionAction(leaveId: Int, action: String) = api.sectionAction(
        com.auraface.auraface_app.data.network.model.LeaveActionRequest(leaveId, action)
    )
    suspend fun getHodLeaves() = api.getHodLeaves()
    suspend fun hodAction(leaveId: Int, approve: Boolean) = api.hodAction(
        com.auraface.auraface_app.data.network.model.HodActionRequest(leaveId, approve)
    )

    suspend fun getSubjectHistory(subjectId: String): List<Map<String, Any>> {
        return try {
            val response = api.getSubjectHistory(subjectId)
            if (response.success == true) { 
                 response.data?.map { record ->
                     mapOf(
                         "date" to record.date,
                         "period" to record.period,
                         "total_present" to record.total_present,
                         "total_absent" to record.total_absent,
                         "present" to record.present,
                         "absent" to record.absent
                     )
                 } ?: emptyList()
            } else {
                 emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
             emptyList()
        }
    }

    suspend fun assignHod(teacherId: Int, department: String) = api.assignHod(teacherId, department)
    suspend fun assignClassTeacher(teacherId: Int, department: String, year: Int, semester: Int, section: String) = 
        api.assignClassTeacher(teacherId, department, year, semester, section)

    suspend fun changePassword(newPassword: String) = api.changePassword(mapOf("new_password" to newPassword))
    suspend fun getTimetable() = api.getTimetable()

    suspend fun createMakeupClass(req: com.auraface.auraface_app.data.network.model.MakeupClassCreate) {
        val res = api.createMakeupClass(req)
        if (!res.success) throw Exception(res.error?.message ?: "Failed to schedule class")
    }

    suspend fun addMarks(req: com.auraface.auraface_app.data.network.model.MarksCreateRequest) = api.addMarks(req)

    suspend fun getSubjectMarks(subjectId: String) = try { api.getSubjectMarks(subjectId) } catch (e: Exception) { emptyList() }

    suspend fun addMarksBulk(bulk: com.auraface.auraface_app.data.network.model.MarksBulkCreate) = api.addMarksBulk(bulk)

    suspend fun getStudentsForClass(department: String, year: Int, section: String) = api.getStudentsForClass(department, year, section)

    suspend fun getSubjects(department: String?, year: Int?, section: String?) = api.getSubjects(department, year, section)

    suspend fun getProctorMeetings(studentId: Int?) = api.getProctorMeetings(studentId)
    
    suspend fun addProctorMeeting(meeting: com.auraface.auraface_app.data.network.model.ProctorMeetingCreate) = api.addProctorMeeting(meeting)

    suspend fun getStudentResults(studentId: Int) = api.getStudentResults(studentId)

    suspend fun sendSectionMessage(message: String, studentId: Int? = null, department: String? = null, year: Int? = null, section: String? = null) {
        val body = com.auraface.auraface_app.data.network.model.SectionMessageCreate(
            message = message,
            student_id = studentId,
            department = department,
            year = year,
            section = section
        )
        api.sendSectionMessage(body)
    }

    suspend fun getNotifications(): List<com.auraface.auraface_app.data.network.model.Notification> {
        return try { api.getNotifications() } catch(e: Exception) { emptyList() }
    }

    suspend fun getSentMessages(): List<com.auraface.auraface_app.data.network.model.SentMessage> {
        return try { api.getSentMessages() } catch(e: Exception) { emptyList() }
    }

    suspend fun searchStudents(
        year: Int? = null,
        department: String? = null,
        semester: Int? = null,
        section: String? = null,
        query: String? = null
    ): List<com.auraface.auraface_app.data.network.api.TeacherStudentSearchItem> {
        return try {
            api.searchStudents(
                year = year,
                department = department,
                semester = semester,
                section = section,
                query = query
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getStudentProfileDetail(studentId: Int): Result<com.auraface.auraface_app.data.network.api.TeacherStudentProfileDetail> {
        return runCatching {
            api.getStudentProfileDetail(studentId)
        }
    }
}
