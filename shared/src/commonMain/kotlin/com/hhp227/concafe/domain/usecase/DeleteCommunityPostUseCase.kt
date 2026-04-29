package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CommunityPostEvent
import com.hhp227.concafe.domain.event.publisher.CommunityPostEventPublisher
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class DeleteCommunityPostUseCase(
    private val authRepository: AuthRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val communityPostEventPublisher: CommunityPostEventPublisher
) {
    suspend operator fun invoke(postId: String): AppResult<Unit> {
        authRepository.getCurrentUser() ?: return AppResult.Failure(AppError.Unauthorized)
        if (postId.isBlank()) return AppResult.Failure(AppError.NotFound)
        return try {
            communityPostRepository.deleteCommunityPost(postId)
            communityPostEventPublisher.publish(CommunityPostEvent.Deleted(postId))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
