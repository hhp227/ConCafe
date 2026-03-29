package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.NotificationRepository

class SendFanAnnouncementUseCase(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        userId: String,
        cafeId: String,
        castId: String,
        title: String,
        body: String
    ): AppResult<Unit> {
        val normalizedTitle = title.trim()
        val normalizedBody = body.trim()

        if (normalizedTitle.isEmpty()) {
            return AppResult.Failure(AppError.ValidationFailed("fan announcement title is required"))
        }
        if (normalizedBody.isEmpty()) {
            return AppResult.Failure(AppError.ValidationFailed("fan announcement body is required"))
        }
        if (normalizedTitle.length > 50) {
            return AppResult.Failure(AppError.ValidationFailed("fan announcement title is too long"))
        }
        if (normalizedBody.length > 300) {
            return AppResult.Failure(AppError.ValidationFailed("fan announcement body is too long"))
        }
        return try {
            notificationRepository.sendFanAnnouncement(
                userId = userId,
                cafeId = cafeId,
                castId = castId,
                title = normalizedTitle,
                body = normalizedBody
            )
            AppResult.Success(Unit)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid fan announcement request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
