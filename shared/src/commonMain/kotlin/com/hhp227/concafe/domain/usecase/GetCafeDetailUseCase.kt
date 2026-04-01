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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
            val loaded = coroutineScope {
                val currentUserDeferred = async {
                    authRepository.getCurrentUser()
                }
                val detailDeferred = async {
                    normalizeDetail(cafeRepository.getCafeDetail(cafeId))
                }
                val reviewPageDeferred = async {
                    reviewRepository.getCafeReviews(
                        cafeId = cafeId,
                        cursor = null,
                        pageSize = INITIAL_REVIEW_PAGE_SIZE
                    )
                }

                Triple(
                    currentUserDeferred.await(),
                    detailDeferred.await(),
                    reviewPageDeferred.await()
                )
            }
            val currentUser = loaded.first
            val detail = loaded.second
            val reviewPage = loaded.third
            val currentDate = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()
            val secondary = coroutineScope {
                val workingCastIdsDeferred = async {
                    castRepository.getWorkingCastIdsByCafeAndDate(
                        cafeId = cafeId,
                        date = currentDate
                    )
                }
                val isFavoriteDeferred = async {
                    if (currentUser != null) {
                        cafeRepository.isFavorite(currentUser.id, cafeId)
                    } else {
                        false
                    }
                }
                val isVisitVerifiedDeferred = async {
                    if (currentUser != null) {
                        visitRepository.hasVerifiedVisitAtCafe(userId = currentUser.id, cafeId = cafeId)
                    } else {
                        false
                    }
                }

                Triple(
                    workingCastIdsDeferred.await(),
                    isFavoriteDeferred.await(),
                    isVisitVerifiedDeferred.await()
                )
            }
            val workingCastIds = secondary.first
            val castItems = detail.casts.map { cast ->
                CafeDetailCast(
                    cast = cast,
                    isWorking = workingCastIds.contains(cast.id)
                )
            }
            val isFavorite = secondary.second
            val isVisitVerified = secondary.third
            val castNameById = detail.casts.associateBy({ cast -> cast.id }, { cast -> cast.name })
            val reviewUserIds = reviewPage.items
                .map { review -> review.userId }
                .distinct()
            val userNicknameById = coroutineScope {
                reviewUserIds.associateWith { userId ->
                    async {
                        runCatching { userRepository.getUser(userId).nickname }
                            .getOrNull()
                    }
                }.mapValues { (_, deferredNickname) ->
                    deferredNickname.await()
                }
            }
            val reviewItems = reviewPage.items.map { review ->
                val userNickname = review.userNickname
                    .takeIf { nickname -> nickname.isNotBlank() }
                    ?: userNicknameById[review.userId]
                    ?: UNKNOWN_USER_NICKNAME
                val verified = review.visitVerified
                val taggedCastNames = review.taggedCastIds.mapNotNull { castId -> castNameById[castId] }

                CafeDetailReview(
                    id = review.id,
                    userId = review.userId,
                    userNickname = userNickname,
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
                    isVisitVerified = isVisitVerified,
                    currentUserId = currentUser?.id
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
        private const val UNKNOWN_USER_NICKNAME = "알 수 없음"
    }
}
