package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.model.CafeDetailCast
import com.hhp227.concafe.domain.model.CafeDetailFeed
import com.hhp227.concafe.domain.model.CafeDetailReview
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository
import com.hhp227.concafe.domain.repository.VisitRepository

class GetCafeDetailUseCase(
    private val authRepository: AuthRepository,
    private val cafeRepository: CafeRepository,
    private val castRepository: CastRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val visitRepository: VisitRepository
) {
    suspend operator fun invoke(cafeId: String): AppResult<CafeDetailFeed> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            val detail = normalizeDetail(cafeRepository.getCafeDetail(cafeId))
            val currentDate = "2026-03-08"
            val castItems = detail.casts.map { cast ->
                val castDetail = castRepository.getCastDetail(cast.id)
                val isWorking = castDetail.schedule.any { it.date == currentDate }

                CafeDetailCast(
                    cast = cast,
                    isWorking = isWorking
                )
            }
            val reviewPage = reviewRepository.getCafeReviews(
                cafeId = cafeId,
                cursor = null,
                pageSize = INITIAL_REVIEW_PAGE_SIZE
            )
            val isFavorite = if (currentUser != null) {
                cafeRepository.isFavorite(currentUser.id, cafeId)
            } else {
                false
            }
            val visitItemsByUserId = buildMap {
                currentUser?.id?.let { userId ->
                    put(
                        userId,
                        visitRepository.getVisits(userId = userId, cursor = null, pageSize = 20).items
                    )
                }

                reviewPage.items
                    .map { it.userId }
                    .distinct()
                    .filterNot { containsKey(it) }
                    .forEach { userId ->
                        put(
                            userId,
                            visitRepository.getVisits(userId = userId, cursor = null, pageSize = 20).items
                        )
                    }
            }
            val isVisitVerified = if (currentUser != null) {
                visitItemsByUserId[currentUser.id]
                    .orEmpty()
                    .any { it.cafeId == cafeId && it.verified }
            } else {
                false
            }
            val castNameById = detail.casts.associateBy({ cast -> cast.id }, { cast -> cast.name })
            val reviewItems = reviewPage.items.map { review ->
                val user = userRepository.getUser(review.userId)
                val verified = visitItemsByUserId[review.userId]
                    .orEmpty()
                    .any { it.cafeId == cafeId && it.verified }
                val taggedCastNames = review.taggedCastIds.mapNotNull { castId -> castNameById[castId] }

                CafeDetailReview(
                    id = review.id,
                    userNickname = user.nickname,
                    rating = review.rating,
                    content = review.content,
                    taggedCastNames = taggedCastNames,
                    likeCount = review.likeCount,
                    createdDate = review.createdAt.take(10),
                    verified = verified
                )
            }

            AppResult.Success(
                CafeDetailFeed(
                    detail = detail,
                    casts = castItems,
                    reviews = reviewItems,
                    reviewsNextCursor = reviewPage.nextCursor,
                    canLoadMoreReviews = reviewPage.hasNext,
                    isFavorite = isFavorite,
                    isLoggedIn = currentUser != null,
                    isVisitVerified = isVisitVerified
                )
            )
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    private fun normalizeDetail(detail: CafeDetail): CafeDetail {
        val filteredImages = detail.images.filter { it.isNotBlank() }
        val normalizedImages = if (filteredImages.isNotEmpty()) {
            filteredImages
        } else if (detail.cafe.thumbnailImage != null && detail.cafe.thumbnailImage.isNotBlank()) {
            listOf(detail.cafe.thumbnailImage)
        } else {
            listOf("")
        }
        return detail.copy(images = normalizedImages)
    }

    companion object {
        private const val INITIAL_REVIEW_PAGE_SIZE = 15
    }
}
