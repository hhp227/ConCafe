package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.NotificationRepository

class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser != null) {
                runCatching {
                    notificationRepository.disableAllPushTokens(currentUser.id)
                }
            } else {
                Unit
            }
            AppResult.Success(authRepository.signOut())
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "sign out failed"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
