package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.NoticeRepository

class ToggleCafeEventLikeUseCase(
    private val authRepository: AuthRepository,
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(cafeId: String, eventId: String): AppResult<Boolean> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            val isLiked = noticeRepository.toggleCafeEventLike(cafeId, eventId, currentUser.id)
            AppResult.Success(isLiked)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
