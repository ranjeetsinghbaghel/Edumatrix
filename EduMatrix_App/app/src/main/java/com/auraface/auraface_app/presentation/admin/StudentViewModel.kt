package com.auraface.auraface_app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.auraface.auraface_app.data.network.api.StudentApi
import com.auraface.auraface_app.domain.model.Student
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val studentApi: StudentApi
) : ViewModel() {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadStudents() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _students.value = studentApi.getStudents()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun createPartFromString(value: String): RequestBody {
        return value.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    fun addStudent(student: Student, imageFile: java.io.File?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val namePart = createPartFromString(student.name)
                val rollPart = createPartFromString(student.roll_no)
                val deptPart = createPartFromString(student.department)
                val yearPart = createPartFromString(student.year.toString())
                val semPart = student.semester?.let { createPartFromString(it.toString()) }
                val secPart = createPartFromString(student.section)
                val programPart = student.program?.let { createPartFromString(it) }
                
                val emailPart = student.email?.let { createPartFromString(it) }
                val mobilePart = student.mobile?.let { createPartFromString(it) }
                val gNamePart = student.guardian_name?.let { createPartFromString(it) }
                val gEmailPart = student.guardian_email?.let { createPartFromString(it) }
                val gMobilePart = student.guardian_mobile?.let { createPartFromString(it) }

                val imagePart = imageFile?.let {
                    val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                    okhttp3.MultipartBody.Part.createFormData("image", it.name, requestFile)
                }

                studentApi.addStudent(
                    name = namePart,
                    rollNo = rollPart,
                    department = deptPart,
                    year = yearPart,
                    semester = semPart,
                    section = secPart,
                    program = programPart,
                    email = emailPart,
                    mobile = mobilePart,
                    guardianName = gNamePart,
                    guardianEmail = gEmailPart,
                    guardianMobile = gMobilePart,
                    image = imagePart
                )
                loadStudents()
            } catch (e: Exception) {
                if (e.message?.contains("400") == true) {
                     _errorMessage.value = "Error: Roll Number/Username already exists!"
                } else {
                     _errorMessage.value = "Failed to add: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStudent(student: Student, imageFile: java.io.File?) {
         viewModelScope.launch {
            _isLoading.value = true
            try {
                val namePart = createPartFromString(student.name)
                val rollPart = createPartFromString(student.roll_no)
                val deptPart = createPartFromString(student.department)
                val yearPart = createPartFromString(student.year.toString())
                val semPart = student.semester?.let { createPartFromString(it.toString()) }
                val secPart = createPartFromString(student.section)
                val programPart = student.program?.let { createPartFromString(it) }
                
                val emailPart = student.email?.let { createPartFromString(it) }
                val mobilePart = student.mobile?.let { createPartFromString(it) }
                val gNamePart = student.guardian_name?.let { createPartFromString(it) }
                val gEmailPart = student.guardian_email?.let { createPartFromString(it) }
                val gMobilePart = student.guardian_mobile?.let { createPartFromString(it) }

                val imagePart = imageFile?.let {
                    val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                    okhttp3.MultipartBody.Part.createFormData("image", it.name, requestFile)
                }

                studentApi.updateStudent(
                    id = student.id,
                    name = namePart,
                    rollNo = rollPart,
                    department = deptPart,
                    year = yearPart,
                    semester = semPart,
                    section = secPart,
                    program = programPart,
                    email = emailPart,
                    mobile = mobilePart,
                    guardianName = gNamePart,
                    guardianEmail = gEmailPart,
                    guardianMobile = gMobilePart,
                    image = imagePart
                )
                loadStudents()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteStudent(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                studentApi.deleteStudent(id)
                loadStudents()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
