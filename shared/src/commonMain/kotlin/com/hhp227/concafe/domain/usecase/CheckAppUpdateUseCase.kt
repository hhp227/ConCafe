package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.AppUpdateInfo
import com.hhp227.concafe.domain.repository.AppUpdateRepository

class CheckAppUpdateUseCase(
    private val appUpdateRepository: AppUpdateRepository
) {
    suspend operator fun invoke(
        storePlatform: String,
        storeId: String,
        currentVersion: String
    ): AppResult<AppUpdateInfo?> {
        return try {
            AppResult.Success(
                appUpdateRepository.getAvailableUpdate(
                    storePlatform = storePlatform,
                    storeId = storeId,
                    currentVersion = currentVersion
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.NetworkError(e.message))
        }
    }
}
