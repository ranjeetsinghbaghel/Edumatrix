package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.auraface.auraface_app.presentation.admin.AdminViewModel

@Composable
fun AdminAttendanceScreen(viewModel: AdminViewModel = hiltViewModel()) {

    val attendance = viewModel.attendance

    LazyColumn {
        items(attendance) { record ->
            Column {
                Text(text = record.date ?: "N/A")
                Text(text = record.status ?: "Unknown")
            }
        }
    }
}

