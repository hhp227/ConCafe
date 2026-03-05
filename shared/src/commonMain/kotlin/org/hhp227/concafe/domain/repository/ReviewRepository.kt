package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.common.PagedResult
import org.hhp227.concafe.domain.model.Review

interface ReviewRepository {
    suspend fun getCafeReviews(cafeId: String, cursor: String?, pageSize: Int): PagedResult<Review>

    suspend fun createReview(
        userId: String,
        cafeId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>
    ): Review

    suspend fun likeReview(userId: String, reviewId: String)

    suspend fun deleteReview(reviewId: String, requesterId: String)
}
