package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserNotificationSettings
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.NotificationRepository

class UpdateNotificationSettingsUseCase(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(settings: UserNotificationSettings): AppResult<UserNotificationSettings> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else {
                val updated = notificationRepository.updateNotificationSettings(currentUser.id, settings)
                AppResult.Success(updated)
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
