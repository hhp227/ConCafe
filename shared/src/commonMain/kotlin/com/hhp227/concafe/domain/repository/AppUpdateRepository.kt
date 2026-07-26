package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.AppUpdateInfo

interface AppUpdateRepository {
    suspend fun getAvailableUpdate(
        storePlatform: String,
        storeId: String,
        currentVersion: String
    ): AppUpdateInfo?
}
