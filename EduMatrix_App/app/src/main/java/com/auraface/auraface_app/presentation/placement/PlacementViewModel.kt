package com.auraface.auraface_app.presentation.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.repository.PlacementRepository
import com.auraface.auraface_app.data.remote.dto.StudentReadinessDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacementViewModel @Inject constructor(
    private val repository: PlacementRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PlacementState>(PlacementState.Loading)
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = PlacementState.Loading
            try {
                val data = repository.getReadinessProfile()
                _state.value = PlacementState.Success(data)
            } catch (e: Exception) {
                _state.value = PlacementState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun addSkill(name: String, level: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) {
                    url = repository.uploadDocument(file)
                }
                repository.addSkill(name, level, url)
                loadData() // Refresh
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun addProject(title: String, description: String, techStack: String, status: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) {
                    url = repository.uploadDocument(file)
                }
                repository.addProject(title, description, techStack, status, url)
                loadData()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
    
    fun addCertification(name: String, issuer: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) {
                    url = repository.uploadDocument(file)
                }
                repository.addCertification(name, issuer, url)
                loadData()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
    
    fun addInternship(company: String, role: String, startDate: String, endDate: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) {
                    url = repository.uploadDocument(file)
                }
                repository.addInternship(company, role, startDate, endDate, url)
                loadData()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
    
    fun addEvent(name: String, type: String, date: String, file: java.io.File? = null) {
        viewModelScope.launch {
             try {
                 var url: String? = null
                 if (file != null) {
                     url = repository.uploadDocument(file)
                 }
                 repository.addEvent(name, type, date, url)
                 loadData()
             } catch (e: Exception) {
                 // handle error
             }
         }
    }

    fun deleteSkill(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteSkill(id)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun deleteCertification(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCertification(id)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun deleteInternship(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteInternship(id)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun deleteProject(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteProject(id)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun deleteEvent(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(id)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun editSkill(id: Int, name: String, level: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) { url = repository.uploadDocument(file) }
                repository.editSkill(id, name, level, url)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun editProject(id: Int, title: String, description: String, techStack: String, status: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) { url = repository.uploadDocument(file) }
                repository.editProject(id, title, description, techStack, status, url)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun editCertification(id: Int, name: String, issuer: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) { url = repository.uploadDocument(file) }
                repository.editCertification(id, name, issuer, url)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun editInternship(id: Int, company: String, role: String, startDate: String, endDate: String, file: java.io.File? = null) {
        viewModelScope.launch {
            try {
                var url: String? = null
                if (file != null) { url = repository.uploadDocument(file) }
                repository.editInternship(id, company, role, startDate, endDate, url)
                loadData()
            } catch (e: Exception) {}
        }
    }

    fun editEvent(id: Int, name: String, type: String, date: String, file: java.io.File? = null) {
        viewModelScope.launch {
             try {
                 var url: String? = null
                 if (file != null) { url = repository.uploadDocument(file) }
                 repository.editEvent(id, name, type, date, url)
                 loadData()
             } catch (e: Exception) {}
         }
    }
}

sealed class PlacementState {
    object Loading : PlacementState()
    data class Success(val data: StudentReadinessDto) : PlacementState()
    data class Error(val message: String) : PlacementState()
}
