package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeDetailReview
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class GetCafeReviewPageUseCase(
    private val cafeRepository: CafeRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        cafeId: String,
        cursor: String?,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): AppResult<PagedResult<CafeDetailReview>> {
        return try {
            val loaded = coroutineScope {
                val detailDeferred = async {
                    cafeRepository.getCafeDetail(cafeId)
                }
                val reviewsDeferred = async {
                    reviewRepository.getCafeReviews(
                        cafeId = cafeId,
                        cursor = cursor,
                        pageSize = pageSize
                    )
                }

                detailDeferred.await() to reviewsDeferred.await()
            }
            val detail = loaded.first
            val castNameById = detail.casts.associate { cast -> cast.id to cast.name }
            val reviews = loaded.second
            val reviewUserIds = reviews.items
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

            AppResult.Success(
                PagedResult(
                    items = reviews.items.map { review ->
                        val userNickname = review.userNickname
                            .takeIf { nickname -> nickname.isNotBlank() }
                            ?: userNicknameById[review.userId]
                            ?: UNKNOWN_USER_NICKNAME
                        val verified = review.visitVerified
                        val taggedCastNames = review.taggedCastIds.mapNotNull { castId -> castNameById[castId] }

                        CafeDetailReview(
                            id = review.id,
                            userNickname = userNickname,
                            rating = review.rating,
                            content = review.content,
                            taggedCastNames = taggedCastNames,
                            likeCount = review.likeCount,
                            createdDate = review.createdAt.take(10),
                            verified = verified
                        )
                    },
                    nextCursor = reviews.nextCursor,
                    hasNext = reviews.hasNext
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

    companion object {
        const val DEFAULT_PAGE_SIZE = 15
        private const val UNKNOWN_USER_NICKNAME = "알 수 없음"
    }
}
