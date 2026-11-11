package com.auraface.auraface_app.presentation.gallery

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.remote.dto.GalleryFolderDto
import com.auraface.auraface_app.data.remote.dto.GalleryImageDto
import com.auraface.auraface_app.data.repository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: GalleryRepository,
    private val tokenManager: com.auraface.auraface_app.data.local.preferences.TokenManager 
) : ViewModel() {

    private val _folders = MutableStateFlow<List<GalleryFolderDto>>(emptyList())
    val folders: StateFlow<List<GalleryFolderDto>> = _folders

    private val _images = MutableStateFlow<List<GalleryImageDto>>(emptyList())
    val images: StateFlow<List<GalleryImageDto>> = _images

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _uploadProgress = MutableStateFlow(false)
    val uploadProgress: StateFlow<Boolean> = _uploadProgress

    init {
        loadFolders()
    }
    
    fun getRole() = tokenManager.getRole() ?: "student"

    fun loadFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _folders.value = repository.getFolders()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.createFolder(name)
                loadFolders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteFolder(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteFolder(id)
                loadFolders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun loadImages(folderId: Int) {
        viewModelScope.launch {
             _isLoading.value = true
             try {
                 _images.value = repository.getImages(folderId)
             } catch(e: Exception) {
                 e.printStackTrace()
             } finally {
                _isLoading.value = false
             }
        }
    }

    fun uploadImage(folderId: Int, file: File) {
        viewModelScope.launch {
            _uploadProgress.value = true
            try {
                repository.uploadImage(folderId, file)
                loadImages(folderId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uploadProgress.value = false
            }
        }
    }

    fun approveImage(id: Int, folderId: Int) {
        viewModelScope.launch {
            try {
                repository.approveImage(id)
                loadImages(folderId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteImage(id: Int, folderId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteImage(id)
                loadImages(folderId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
