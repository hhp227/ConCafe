package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.VisitRepository

class CreateReviewUseCase(
    private val authRepository: AuthRepository,
    private val reviewRepository: ReviewRepository,
    private val visitRepository: VisitRepository,
    private val reviewEventPublisher: ReviewEventPublisher
) {
    suspend operator fun invoke(
        cafeId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): AppResult<Review> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                AppResult.Failure(AppError.Unauthorized)
            } else if (cafeId.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            } else if (rating <= 0f) {
                AppResult.Failure(AppError.ValidationFailed("rating is required"))
            } else if (content.isBlank()) {
                AppResult.Failure(AppError.ValidationFailed("review content is required"))
            } else {
                val visitId = visitRepository.getVisits(
                    userId = currentUser.id,
                    cursor = null,
                    pageSize = 50
                ).items
                    .filter { item -> item.cafeId == cafeId }
                    .maxByOrNull { item -> item.visitedAt }
                    ?.id
                    .orEmpty()
                val created = reviewRepository.createReview(
                    userId = currentUser.id,
                    cafeId = cafeId,
                    visitId = visitId,
                    rating = rating,
                    content = content.trim(),
                    imageUrls = imageUrls,
                    taggedCastIds = taggedCastIds
                )

                reviewEventPublisher.publish(
                    ReviewEvent.Created(cafeId)
                )
                AppResult.Success(created)
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
