package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class ToggleCommunityPostLikeUseCase(
    private val authRepository: AuthRepository,
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String): AppResult<Boolean> {
        val user = authRepository.getCurrentUser() ?: return AppResult.Failure(AppError.Unauthorized)
        return try {
            val isLiked = communityPostRepository.toggleLike(postId, user.id)
            AppResult.Success(isLiked)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
