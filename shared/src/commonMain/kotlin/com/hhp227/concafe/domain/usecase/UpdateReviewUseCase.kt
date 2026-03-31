package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.ReviewRepository

class UpdateReviewUseCase(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository
) {
    suspend operator fun invoke(
        reviewId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): AppResult<Review> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            val review = reviewRepository.updateReview(
                reviewId = reviewId,
                requesterId = currentUser.id,
                rating = rating,
                content = content,
                imageUrls = imageUrls,
                taggedCastIds = taggedCastIds
            )
            AppResult.Success(review)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
