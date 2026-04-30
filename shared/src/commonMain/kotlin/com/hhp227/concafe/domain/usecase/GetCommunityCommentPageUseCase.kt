package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.Comment
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class GetCommunityCommentPageUseCase(
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String, beforeCursor: String? = null, pageSize: Int = 5): AppResult<PagedResult<Comment>> {
        if (postId.isBlank()) return AppResult.Failure(AppError.NotFound)
        return try {
            AppResult.Success(communityPostRepository.getCommentPage(postId, beforeCursor, pageSize))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
