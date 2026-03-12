package com.hhp227.concafe.data.repository

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
        rating: Float,
        content: String,
        imageUrls: List<String>
    ): Review {
        if (content.isBlank()) {
            throw IllegalArgumentException("review content is required")
        }

        val review = Review(
            id = "review-${dataSource.reviews.size + 1}",
            userId = userId,
            cafeId = cafeId,
            rating = rating,
            content = content,
            imageUrls = imageUrls,
            likeCount = 0,
            createdAt = "2026-03-05T00:00:00Z"
        )
        dataSource.reviews.add(review)
        return review
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
            dataSource.reviews.removeAt(index)
        } else {
            throw Exception("no permission to delete review")
        }
    }
}
