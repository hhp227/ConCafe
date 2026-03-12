package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CafeDetailReview
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository
import com.hhp227.concafe.domain.repository.VisitRepository

class GetCafeReviewPageUseCase(
    private val cafeRepository: CafeRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val visitRepository: VisitRepository
) {
    suspend operator fun invoke(
        cafeId: String,
        cursor: String?,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): AppResult<PagedResult<CafeDetailReview>> {
        return try {
            val detail = cafeRepository.getCafeDetail(cafeId)
            val castNameById = detail.casts.associate { cast -> cast.id to cast.name }
            val reviews = reviewRepository.getCafeReviews(
                cafeId = cafeId,
                cursor = cursor,
                pageSize = pageSize
            )

            AppResult.Success(
                PagedResult(
                    items = reviews.items.map { review ->
                        val user = userRepository.getUser(review.userId)
                        val visits = visitRepository.getVisits(
                            userId = review.userId,
                            cursor = null,
                            pageSize = 20
                        ).items
                        val verified = visits.any { it.cafeId == cafeId && it.verified }
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
    }
}
