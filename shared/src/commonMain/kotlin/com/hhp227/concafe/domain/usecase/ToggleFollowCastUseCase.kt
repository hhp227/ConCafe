package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository

class ToggleFollowCastUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository,
    private val castEventPublisher: CastEventPublisher
) {
    suspend operator fun invoke(castId: String): AppResult<Boolean> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            val isFollowing = castRepository.isFollowing(currentUser.id, castId)
            val previousCast = castRepository.getCastsByIds(listOf(castId)).firstOrNull()

            if (isFollowing) {
                castRepository.unfollowCast(currentUser.id, castId)
            } else {
                castRepository.followCast(currentUser.id, castId)
            }
            val updatedCast = castRepository.getCastsByIds(listOf(castId)).firstOrNull()
            if (updatedCast != null) {
                val adjustedFollowerCount = previousCast?.followerCount?.let { previousCount ->
                    if (isFollowing) {
                        minOf(updatedCast.followerCount, (previousCount - 1).coerceAtLeast(0))
                    } else {
                        maxOf(updatedCast.followerCount, previousCount + 1)
                    }
                } ?: updatedCast.followerCount

                castEventPublisher.publish(
                    CastEvent.Updated(
                        cafeId = updatedCast.cafeId,
                        cast = updatedCast.copy(followerCount = adjustedFollowerCount),
                        isFollowing = !isFollowing
                    )
                )
            }
            AppResult.Success(!isFollowing)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
