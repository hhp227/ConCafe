package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.MyInfoDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.data.source.ReviewDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.repository.ReviewRepository
import kotlinx.datetime.Clock

class ReviewRepositoryImpl(
    private val reviewDataSource: ReviewDataSource,
    private val pagingDataSource: PagingDataSource,
    private val myInfoDataSource: MyInfoDataSource
) : ReviewRepository {
    override suspend fun getCafeReviews(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Review> {
        val items = reviewDataSource.reviews
            .filter { it.cafeId == cafeId }
            .sortedByDescending { it.createdAt }
        return pagingDataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun createReview(
        userId: String,
        cafeId: String,
        visitId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review {
        if (content.isBlank()) {
            throw IllegalArgumentException("review content is required")
        }

        val review = Review(
            id = nextEntityId("review"),
            userId = userId,
            cafeId = cafeId,
            visitId = visitId,
            rating = rating,
            content = content,
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds,
            likeCount = 0,
            createdAt = nowIsoUtc()
        )
        reviewDataSource.reviews.add(review)
        reviewDataSource.refreshReviewProjections(
            cafeId = cafeId,
            taggedCastIds = taggedCastIds
        )
        return review
    }

    override suspend fun hasReviewForVisit(visitId: String): Boolean {
        return reviewDataSource.reviews.any { it.visitId == visitId }
    }

    override suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean {
        return myInfoDataSource.dismissedReviewPromptVisitIdsByUser[userId]?.contains(visitId) == true
    }

    override suspend fun dismissReviewPrompt(userId: String, visitId: String) {
        val dismissedVisitIds = myInfoDataSource.dismissedReviewPromptVisitIdsByUser
            .getOrPut(userId) { mutableSetOf() }
        dismissedVisitIds.add(visitId)
    }

    override suspend fun likeReview(userId: String, reviewId: String) {
        val index = reviewDataSource.reviews.indexOfFirst { it.id == reviewId }

        if (index == -1) {
            throw NoSuchElementException("review not found")
        }

        val current = reviewDataSource.reviews[index]
        reviewDataSource.reviews[index] = current.copy(likeCount = current.likeCount + 1)
    }

    override suspend fun deleteReview(reviewId: String, requesterId: String) {
        val index = reviewDataSource.reviews.indexOfFirst {
            it.id == reviewId && it.userId == requesterId
        }

        if (index == -1) {
            throw IllegalStateException("no permission to delete review")
        }

        val deletedReview = reviewDataSource.reviews.removeAt(index)
        reviewDataSource.refreshReviewProjections(
            cafeId = deletedReview.cafeId,
            taggedCastIds = deletedReview.taggedCastIds
        )
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
