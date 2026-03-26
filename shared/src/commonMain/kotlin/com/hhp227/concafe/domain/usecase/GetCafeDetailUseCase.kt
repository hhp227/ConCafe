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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
            val reviewPage = reviewRepository.getCafeReviews(
                cafeId = cafeId,
                cursor = null,
                pageSize = INITIAL_REVIEW_PAGE_SIZE
            )
            val currentDate = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()
            val workingCastIds = castRepository.getWorkingCastIdsByCafeAndDate(
                cafeId = cafeId,
                date = currentDate
            )
            val castItems = detail.casts.map { cast ->
                CafeDetailCast(
                    cast = cast,
                    isWorking = workingCastIds.contains(cast.id)
                )
            }
            val isFavorite = if (currentUser != null) {
                cafeRepository.isFavorite(currentUser.id, cafeId)
            } else {
                false
            }
            val isVisitVerified = if (currentUser != null) {
                visitRepository.hasVerifiedVisitAtCafe(userId = currentUser.id, cafeId = cafeId)
            } else {
                false
            }
            val castNameById = detail.casts.associateBy({ cast -> cast.id }, { cast -> cast.name })
            val reviewUsersById = reviewPage.items
                .map { review -> review.userId }
                .distinct()
                .associateWith { userId -> userRepository.getUser(userId) }
            val reviewItems = reviewPage.items.map { review ->
                val user = reviewUsersById[review.userId] ?: userRepository.getUser(review.userId)
                val verified = review.visitVerified
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
