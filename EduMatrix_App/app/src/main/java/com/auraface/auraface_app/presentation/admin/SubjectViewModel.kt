package com.auraface.auraface_app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.api.TeacherApi
import com.auraface.auraface_app.domain.model.Subject
import com.auraface.auraface_app.domain.model.Teacher
import com.auraface.auraface_app.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val repo: SubjectRepository,
    private val teacherApi: TeacherApi
) : ViewModel() {

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    private val _teachers = MutableStateFlow<List<Teacher>>(emptyList())
    val teachers: StateFlow<List<Teacher>> = _teachers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _subjects.value = repo.getSubjects()
                _teachers.value = teacherApi.getTeachers()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addSubject(subject: Subject) {
        viewModelScope.launch {
            try {
                repo.addSubject(subject)
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("SubjectViewModel", "Error adding subject: ${e.message}", e)
            }
        }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            try {
                repo.updateSubject(subject)
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("SubjectViewModel", "Error updating subject: ${e.message}", e)
            }
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteSubject(id)
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("SubjectViewModel", "Error deleting subject: ${e.message}", e)
            }
        }
    }

}
