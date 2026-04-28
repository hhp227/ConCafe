package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class CheckCommunityPostLikedUseCase(
    private val authRepository: AuthRepository,
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String): AppResult<Boolean> {
        val user = authRepository.getCurrentUser() ?: return AppResult.Success(false)
        return try {
            AppResult.Success(communityPostRepository.isLikedByUser(postId, user.id))
        } catch (e: Exception) {
            AppResult.Success(false)
        }
    }
}
