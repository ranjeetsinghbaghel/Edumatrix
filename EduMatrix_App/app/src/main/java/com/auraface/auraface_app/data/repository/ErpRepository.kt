package com.auraface.auraface_app.data.repository

import com.auraface.auraface_app.data.network.api.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErpRepository @Inject constructor(
    private val erpApi: ErpApi
) {
    suspend fun getStatus(): Result<ErpStatusResponse> = runCatching {
        erpApi.getStatus()
    }

    suspend fun getCaptcha(): Result<ErpCaptchaResponse> = runCatching {
        erpApi.getCaptcha()
    }

    suspend fun getStudentErpData(rollNo: String): Result<ErpFullReport> = runCatching {
        erpApi.getStudentErpData(rollNo)
    }

    suspend fun syncData(request: ErpSyncRequest): Result<ErpSyncResponse> = runCatching {
        erpApi.syncData(request)
    }
}
