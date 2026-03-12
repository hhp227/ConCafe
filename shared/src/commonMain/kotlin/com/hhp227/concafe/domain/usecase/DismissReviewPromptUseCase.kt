package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.ReviewRepository

class DismissReviewPromptUseCase(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(visitId: String): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null || visitId.isBlank()) {
                AppResult.Success(Unit)
            } else {
                reviewRepository.dismissReviewPrompt(currentUser.id, visitId)
                AppResult.Success(Unit)
            }
        } catch (e: Exception) {
            AppResult.Success(Unit)
        }
    }
}
