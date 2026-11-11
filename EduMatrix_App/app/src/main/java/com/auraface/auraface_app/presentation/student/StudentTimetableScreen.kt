package com.auraface.auraface_app.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.auraface.auraface_app.presentation.common.PremiumTimetable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTimetableScreen(
    navController: NavController,
    viewModel: StudentViewModel = hiltViewModel()
) {
    val timetable = viewModel.timetable
    val profile = viewModel.profile

    LaunchedEffect(Unit) {
        viewModel.loadTimetable()
    }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Timetable") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            PremiumTimetable(
                timetable = timetable,
                department = profile?.department,
                year = profile?.year ?: 0,
                semester = viewModel.profile?.semester, // Assuming added to profile, or inferred
                section = profile?.section,
                onSlotClick = null // Student's view only
            )
        }
    }
}
