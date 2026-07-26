package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserNotificationSettings
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.NotificationRepository

class GetNotificationSettingsUseCase(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(): AppResult<UserNotificationSettings> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                val settings = notificationRepository.getNotificationSettings(currentUser.id)
                AppResult.Success(settings)
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
