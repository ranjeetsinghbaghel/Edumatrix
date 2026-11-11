package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.domain.model.ClassRoom
import javax.inject.Inject

class TeacherRepositoryImpl @Inject constructor() {

    fun getClasses(): List<ClassRoom> {
        return listOf(
            ClassRoom("CSE-A"),
            ClassRoom("CSE-B")
        )
    }
}

