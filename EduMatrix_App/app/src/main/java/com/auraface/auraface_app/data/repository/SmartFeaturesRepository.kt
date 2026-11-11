package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.SmartFeaturesApi
import com.auraface.auraface_app.data.remote.dto.*
import javax.inject.Inject

class SmartFeaturesRepository @Inject constructor(
    private val api: SmartFeaturesApi
) {
    suspend fun getNoticeList(): List<NoticeDto> {
        return api.getNotices()
    }

    suspend fun markNoticeRead(id: Int) {
        api.markNoticeRead(id)
    }

    suspend fun getNoticeReaders(id: Int): List<NoticeReaderDto> {
        return api.getNoticeReaders(id)
    }

    suspend fun getAttendanceTrend(): List<TrendPointDto> {
        return api.getTrend()
    }

    suspend fun getStudentRisk(): StudentRiskDto {
        return api.getRisk()
    }

    suspend fun getRequiredClasses(): RequiredClassesDto {
        return api.getRequiredClasses()
    }

    suspend fun askAura(message: String): ChatResponseDto {
        return api.askAura(ChatRequestDto(message))
    }
}
