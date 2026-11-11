package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.presentation.admin.AddQuestionViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionScreen(
    navController: NavController,
    viewModel: AddQuestionViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedFileUri = uri
    }

    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            selectedFileUri = null
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Quiz Question") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (viewModel.successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.successMessage!!)
                        }
                    }
                }

                if (viewModel.errorMessage != null) {
                    Text(
                        text = viewModel.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            item {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SUBJECT", "APTITUDE", "COLLEGE", "FUN").forEach { cat ->
                        FilterChip(
                            selected = viewModel.category == cat,
                            onClick = { viewModel.category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
            
            item {
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
                        FilterChip(
                            selected = viewModel.difficulty == diff,
                            onClick = { viewModel.difficulty = diff },
                            label = { Text(diff) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = viewModel.questionText,
                    onValueChange = { viewModel.questionText = it },
                    label = { Text("Question Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            item {
                Text("Options (Select the correct answer)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OptionInput("A", viewModel.optionA, { viewModel.optionA = it }, viewModel.correctOptionIndex == 0) { viewModel.correctOptionIndex = 0 }
                OptionInput("B", viewModel.optionB, { viewModel.optionB = it }, viewModel.correctOptionIndex == 1) { viewModel.correctOptionIndex = 1 }
                OptionInput("C", viewModel.optionC, { viewModel.optionC = it }, viewModel.correctOptionIndex == 2) { viewModel.correctOptionIndex = 2 }
                OptionInput("D", viewModel.optionD, { viewModel.optionD = it }, viewModel.correctOptionIndex == 3) { viewModel.correctOptionIndex = 3 }
            }
            
            item {
                 OutlinedTextField(
                    value = viewModel.explanation,
                    onValueChange = { viewModel.explanation = it },
                    label = { Text("Explanation (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedFileUri != null) "File Attached" else "No file attached",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedFileUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { launcher.launch("*/*") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileUri != null) "Change File" else "Attach File")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            var file: java.io.File? = null
                            if (selectedFileUri != null) {
                                file = withContext(Dispatchers.IO) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
                                        val tempFile = java.io.File(context.cacheDir, "quiz_file_${System.currentTimeMillis()}")
                                        val outputStream = java.io.FileOutputStream(tempFile)
                                        inputStream?.copyTo(outputStream)
                                        inputStream?.close()
                                        outputStream.close()
                                        tempFile
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            viewModel.submitQuestion(file)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isLoading
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Add Question")
                    }
                }
            }
        }
    }
}

@Composable
fun OptionInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isCorrect: Boolean,
    onSelectCorrect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        RadioButton(
            selected = isCorrect,
            onClick = onSelectCorrect
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Option $label") },
            modifier = Modifier.weight(1f)
        )
    }
}
