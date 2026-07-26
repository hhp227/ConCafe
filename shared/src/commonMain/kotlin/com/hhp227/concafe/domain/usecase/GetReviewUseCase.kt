package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.repository.ReviewRepository

class GetReviewUseCase(
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(reviewId: String): AppResult<Review> {
        return try {
            AppResult.Success(reviewRepository.getReview(reviewId))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
