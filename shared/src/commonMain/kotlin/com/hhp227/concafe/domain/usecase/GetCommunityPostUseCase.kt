package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class GetCommunityPostUseCase(
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String): AppResult<CommunityPost> {
        if (postId.isBlank()) return AppResult.Failure(AppError.NotFound)
        return try {
            AppResult.Success(communityPostRepository.getCommunityPost(postId))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
