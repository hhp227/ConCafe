package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.model.Review
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    suspend fun getCafeReviews(cafeId: String, cursor: String?, pageSize: Int): PagedResult<Review>

    suspend fun createReview(
        userId: String,
        cafeId: String,
        visitId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review

    suspend fun hasReviewForVisit(visitId: String): Boolean

    suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean

    suspend fun dismissReviewPrompt(userId: String, visitId: String)

    suspend fun likeReview(userId: String, reviewId: String)

    suspend fun deleteReview(reviewId: String, requesterId: String)
}
