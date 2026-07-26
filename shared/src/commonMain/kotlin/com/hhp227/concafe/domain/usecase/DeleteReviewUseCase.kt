package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.ReviewRepository

class DeleteReviewUseCase(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val reviewEventPublisher: ReviewEventPublisher
) {
    suspend operator fun invoke(cafeId: String, reviewId: String): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            reviewRepository.deleteReview(reviewId = reviewId, requesterId = currentUser.id)
            reviewEventPublisher.publish(ReviewEvent.Deleted(cafeId = cafeId, reviewId = reviewId))
            AppResult.Success(Unit)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
