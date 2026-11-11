package com.auraface.auraface_app.presentation.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auraface.auraface_app.data.network.model.QuestionCreateRequest
import com.auraface.auraface_app.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject

@HiltViewModel
class AddQuestionViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    var questionText by mutableStateOf("")
    var optionA by mutableStateOf("")
    var optionB by mutableStateOf("")
    var optionC by mutableStateOf("")
    var optionD by mutableStateOf("")
    var correctOptionIndex by mutableIntStateOf(0) // 0=A, 1=B, 2=C, 3=D
    var category by mutableStateOf("SUBJECT")
    var difficulty by mutableStateOf("MEDIUM")
    var explanation by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    fun submitQuestion(file: java.io.File? = null) {
        if (questionText.isBlank() || optionA.isBlank() || optionB.isBlank() || optionC.isBlank() || optionD.isBlank()) {
            errorMessage = "Please fill all required fields"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            successMessage = null

            try {
                var attachmentUrl: String? = null
                
                if (file != null) {
                    val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                    val filePart = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
                    val uploadRes = repository.uploadAttachment(filePart)
                    if (uploadRes.isSuccessful && uploadRes.body() != null) {
                        attachmentUrl = uploadRes.body()?.url
                    } else {
                        errorMessage = "Failed to upload file."
                        return@launch
                    }
                }

                val request = QuestionCreateRequest(
                    category = category,
                    difficulty = difficulty,
                    question_text = questionText,
                    attachment_url = attachmentUrl,
                    options = listOf(optionA, optionB, optionC, optionD),
                    correct_option_index = correctOptionIndex,
                    explanation = if (explanation.isBlank()) null else explanation
                )

                val response = repository.createQuestion(request)
                if (response.isSuccessful) {
                    successMessage = "Question added successfully!"
                    // Reset fields
                    questionText = ""
                    optionA = ""
                    optionB = ""
                    optionC = ""
                    optionD = ""
                    explanation = ""
                    correctOptionIndex = 0
                } else {
                    errorMessage = "Failed to add question: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
