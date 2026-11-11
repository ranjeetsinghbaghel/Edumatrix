package com.auraface.auraface_app.presentation.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.auraface.auraface_app.presentation.admin.AdminViewModel

@Composable
fun AdminStudentListScreen(viewModel: AdminViewModel = hiltViewModel()) {

    val students = viewModel.students

    LazyColumn {
        items(students) { student ->
            Column {
                Text(text = student.name)
                Text(text = student.id.toString())
                Text(text = student.department)
            }
        }
    }
}
