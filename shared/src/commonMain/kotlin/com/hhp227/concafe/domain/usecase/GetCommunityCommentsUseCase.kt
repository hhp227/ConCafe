package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class GetCommunityCommentsUseCase(
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String): AppResult<List<Comment>> {
        if (postId.isBlank()) return AppResult.Failure(AppError.NotFound)
        return try {
            AppResult.Success(communityPostRepository.getComments(postId))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
