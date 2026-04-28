package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class GetCommunityPostPageUseCase(
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(
        cursor: String?,
        pageSize: Int = 20
    ): AppResult<PagedResult<CommunityPost>> {
        return try {
            AppResult.Success(communityPostRepository.getCommunityPostPage(cursor, pageSize))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
