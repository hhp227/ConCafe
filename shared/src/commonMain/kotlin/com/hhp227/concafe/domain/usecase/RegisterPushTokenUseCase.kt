package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.NotificationRepository

class RegisterPushTokenUseCase(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(platform: String, token: String): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            val normalizedPlatform = platform.trim()
            val normalizedToken = token.trim()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (normalizedPlatform.isEmpty()) {
                AppResult.Failure(AppError.ValidationFailed("platform is required"))
            } else if (normalizedToken.isEmpty()) {
                AppResult.Failure(AppError.ValidationFailed("token is required"))
            } else {
                notificationRepository.registerPushToken(
                    userId = currentUser.id,
                    platform = normalizedPlatform,
                    token = normalizedToken
                )
                AppResult.Success(Unit)
            }
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
