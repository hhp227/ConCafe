package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.MyInfoRemoteDataSource
import com.hhp227.concafe.data.source.ReviewRemoteDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Review
import com.hhp227.concafe.domain.repository.ReviewRepository

class ReviewRepositoryImpl(
    private val reviewRemoteDataSource: ReviewRemoteDataSource,
    private val myInfoRemoteDataSource: MyInfoRemoteDataSource
) : ReviewRepository {
    override suspend fun getCafeReviews(
        cafeId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<Review> {
        return reviewRemoteDataSource.fetchCafeReviews(
            cafeId = cafeId,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getReview(reviewId: String): Review {
        return reviewRemoteDataSource.fetchReview(reviewId)
    }

    override suspend fun getRecentTaggedReviews(cafeId: String, castId: String, limit: Int): List<Review> {
        val safeLimit = if (limit > 0) limit else 1
        return reviewRemoteDataSource.fetchRecentTaggedReviews(
            cafeId = cafeId,
            castId = castId,
            limit = safeLimit
        )
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
        return reviewRemoteDataSource.createReview(
            userId = userId,
            cafeId = cafeId,
            visitId = visitId,
            rating = rating,
            content = content,
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds
        )
    }

    override suspend fun updateReview(
        reviewId: String,
        requesterId: String,
        rating: Float,
        content: String,
        imageUrls: List<String>,
        taggedCastIds: List<String>
    ): Review {
        if (content.isBlank()) {
            throw IllegalArgumentException("review content is required")
        }
        return reviewRemoteDataSource.updateReview(
            reviewId = reviewId,
            requesterId = requesterId,
            rating = rating,
            content = content,
            imageUrls = imageUrls,
            taggedCastIds = taggedCastIds
        )
    }

    override suspend fun hasReviewForVisit(visitId: String): Boolean {
        return reviewRemoteDataSource.hasReviewForVisit(visitId)
    }

    override suspend fun isReviewPromptDismissed(userId: String, visitId: String): Boolean {
        return myInfoRemoteDataSource.isReviewPromptDismissed(userId, visitId)
    }

    override suspend fun dismissReviewPrompt(userId: String, visitId: String) {
        myInfoRemoteDataSource.dismissReviewPrompt(userId, visitId)
    }

    override suspend fun likeReview(userId: String, reviewId: String) {
        reviewRemoteDataSource.likeReview(reviewId = reviewId)
    }

    override suspend fun deleteReview(reviewId: String, requesterId: String) {
        reviewRemoteDataSource.deleteReview(
            reviewId = reviewId,
            requesterId = requesterId
        )
    }
}
