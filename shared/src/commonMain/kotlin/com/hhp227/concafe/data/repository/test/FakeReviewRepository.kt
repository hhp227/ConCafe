package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.repository.ReviewRepository

class FakeReviewRepository(
    private val dataSource: ConCafeDataSource
) : ReviewRepository {
    override suspend fun getCafeReviews(cafeId: String, cursor: String?, pageSize: Int): PagedResult<Review> {
        val items = dataSource.reviews.filter { it.cafeId == cafeId }.sortedByDescending { it.createdAt }
        return dataSource.toPaged(items, cursor, pageSize)
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
            id = "review-${dataSource.reviews.size + 1}",
            userId = userId,
            cafeId = cafeId,
            visitId = visitId,
            rating = rating,
            content = content,
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds,
            likeCount = 0,
            createdAt = nextCreatedAt(dataSource.reviews.size)
        )
        dataSource.reviews.add(review)
        dataSource.refreshReviewProjections(
            cafeId = cafeId,
            taggedCastIds = taggedCastIds
        )
        return review
    }

    override suspend fun hasReviewForVisit(visitId: String): Boolean {
        return dataSource.reviews.any { it.visitId == visitId }
    }

    override suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean {
        return dataSource.dismissedReviewPromptVisitIdsByUser[userId]?.contains(visitId) == true
    }

    override suspend fun dismissReviewPrompt(userId: String, visitId: String) {
        val dismissedVisitIds = dataSource.dismissedReviewPromptVisitIdsByUser.getOrPut(userId) { mutableSetOf() }
        dismissedVisitIds.add(visitId)
    }

    override suspend fun likeReview(userId: String, reviewId: String) {
        val index = dataSource.reviews.indexOfFirst { it.id == reviewId }

        if (index == -1) {
            throw NoSuchElementException("review not found")
        } else {
            val current = dataSource.reviews[index]
            dataSource.reviews[index] = current.copy(likeCount = current.likeCount + 1)
        }
    }

    override suspend fun deleteReview(reviewId: String, requesterId: String) {
        val index = dataSource.reviews.indexOfFirst { it.id == reviewId && it.userId == requesterId }

        if (index >= 0) {
            val deletedReview = dataSource.reviews.removeAt(index)
            dataSource.refreshReviewProjections(
                cafeId = deletedReview.cafeId,
                taggedCastIds = deletedReview.taggedCastIds
            )
            //reviewEvent.tryEmit(ReviewEvent.Deleted(deletedReview.cafeId, reviewId))
        } else {
            throw Exception("no permission to delete review")
        }
    }
}

private fun nextCreatedAt(reviewCount: Int): String {
    val second = (reviewCount % 60).toString().padStart(2, '0')
    return "2026-03-12T23:59:${second}Z"
}
