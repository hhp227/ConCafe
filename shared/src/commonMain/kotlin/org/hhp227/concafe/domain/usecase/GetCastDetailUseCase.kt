package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CastDetail
import com.hhp227.concafe.domain.model.CastDetailFeed
import com.hhp227.concafe.domain.model.CastRecentReview
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository
import com.hhp227.concafe.domain.repository.ReviewRepository
import com.hhp227.concafe.domain.repository.UserRepository

class GetCastDetailUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(castId: String): AppResult<CastDetailFeed> {
        return try {
            val currentUser = authRepository.getCurrentUser()
            val detail = normalizeDetail(castRepository.getCastDetail(castId))
            val recentReviews = reviewRepository.getCafeReviews(
                cafeId = detail.cafe.id,
                cursor = null,
                pageSize = 3
            ).items.map { review ->
                val user = userRepository.getUser(review.userId)
                CastRecentReview(
                    id = review.id,
                    userNickname = user.nickname,
                    content = review.content,
                    createdDateLabel = review.createdAt.toRelativeDateLabel()
                )
            }
            val isFollowing = if (currentUser != null) {
                castRepository.isFollowing(currentUser.id, castId)
            } else {
                false
            }

            AppResult.Success(
                CastDetailFeed(
                    detail = detail,
                    recentReviews = recentReviews,
                    isFollowing = isFollowing,
                    isLoggedIn = currentUser != null
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

    private fun normalizeDetail(detail: CastDetail): CastDetail {
        val filteredImages = detail.images.filter { it.isNotBlank() }
        val normalizedImages = if (filteredImages.isNotEmpty()) {
            filteredImages
        } else if (!detail.cast.profileImage.isNullOrBlank()) {
            listOfNotNull(detail.cast.profileImage)
        } else if (!detail.cafe.thumbnailImage.isNullOrBlank()) {
            listOfNotNull(detail.cafe.thumbnailImage)
        } else {
            listOf("")
        }

        return detail.copy(images = normalizedImages)
    }
}

private fun String.toRelativeDateLabel(): String {
    val date = take(10)
    return when (date) {
        "2026-03-09" -> "오늘"
        "2026-03-08" -> "1일 전"
        "2026-03-07" -> "2일 전"
        "2026-03-06" -> "3일 전"
        else -> date
    }
}
