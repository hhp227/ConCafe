package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class DeleteCommunityCommentUseCase(
    private val authRepository: AuthRepository,
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String, commentId: String): AppResult<Unit> {
        authRepository.getCurrentUser() ?: return AppResult.Failure(AppError.Unauthorized)
        return try {
            communityPostRepository.deleteComment(postId, commentId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
