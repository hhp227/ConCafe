package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.ReviewRepository

class ShouldShowReviewPromptUseCase(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(visitId: String): AppResult<Boolean> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null || visitId.isBlank()) {
                AppResult.Success(false)
            } else {
                val alreadyReviewed = reviewRepository.hasReviewForVisit(visitId)
                val dismissed = reviewRepository.isReviewPromptDismissed(currentUser.id, visitId)
                AppResult.Success(!alreadyReviewed && !dismissed)
            }
        } catch (e: Exception) {
            AppResult.Success(false)
        }
    }
}
