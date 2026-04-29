package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class AddCommunityCommentUseCase(
    private val authRepository: AuthRepository,
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String, content: String): AppResult<Comment> {
        val user = authRepository.getCurrentUser() ?: return AppResult.Failure(AppError.Unauthorized)
        if (content.isBlank()) return AppResult.Failure(AppError.ValidationFailed("content is required"))
        return try {
            val comment = communityPostRepository.addComment(postId, user.id, content.trim())
            AppResult.Success(comment)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
