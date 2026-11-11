package com.auraface.auraface_app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.api.TeacherApi
import com.auraface.auraface_app.domain.model.Teacher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val repo: com.auraface.auraface_app.data.repository.TeacherRepository
) : ViewModel() {

    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadTeachers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _teachers.value = repo.getTeachers()
            } catch (e: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addTeacher(teacher: Teacher, imageFile: java.io.File?) {
        viewModelScope.launch {
            try {
                repo.addTeacher(teacher, imageFile)
                loadTeachers()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun updateTeacher(teacher: Teacher, imageFile: java.io.File?) {
        viewModelScope.launch {
            try {
                repo.updateTeacher(teacher, imageFile)
                loadTeachers()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun deleteTeacher(id: Int) {
        viewModelScope.launch {
            try {
                repo.deleteTeacher(id)
                loadTeachers()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun assignHod(teacherId: Int, department: String) {
        viewModelScope.launch {
            try {
                repo.assignHod(teacherId, department)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun assignClassTeacher(teacherId: Int, department: String, year: Int, semester: Int, section: String) {
        viewModelScope.launch {
            try {
                repo.assignClassTeacher(teacherId, department, year, semester, section)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}
