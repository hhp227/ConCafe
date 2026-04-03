package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Review

interface ReviewRemoteDataSource {
    suspend fun fetchCafeReviews(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Review>

    suspend fun fetchReview(reviewId: String): Review

    suspend fun fetchRecentTaggedReviews(cafeId: String, castId: String, limit: Int): List<Review>

    suspend fun createReview(
        userId: String,
        cafeId: String,
        visitId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review

    suspend fun updateReview(
        reviewId: String,
        requesterId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review

    suspend fun hasReviewForVisit(visitId: String): Boolean

    suspend fun likeReview(reviewId: String)

    suspend fun deleteReview(reviewId: String, requesterId: String)
}
