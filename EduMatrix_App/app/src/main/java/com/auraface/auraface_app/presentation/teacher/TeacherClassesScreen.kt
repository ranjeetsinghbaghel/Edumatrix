package com.auraface.auraface_app.presentation.teacher

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.auraface.auraface_app.domain.model.ClassRoom

@Composable
fun TeacherClassesScreen(classes: List<ClassRoom>) {

    LazyColumn {
        items(classes) { cls ->
            Text(text = cls.name)
        }
    }
}
