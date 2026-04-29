package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CommunityPostEvent
import com.hhp227.concafe.domain.event.publisher.CommunityPostEventPublisher
import com.hhp227.concafe.domain.model.CommunityPost
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CommunityPostRepository

class CreateCommunityPostUseCase(
    private val authRepository: AuthRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val communityPostEventPublisher: CommunityPostEventPublisher
) {
    suspend operator fun invoke(
        title: String,
        content: String,
        imageUrls: List<String>
    ): AppResult<CommunityPost> {
        val currentUser = authRepository.getCurrentUser()
            ?: return AppResult.Failure(AppError.Unauthorized)
        if (title.isBlank()) {
            return AppResult.Failure(AppError.ValidationFailed("title is required"))
        }
        if (content.isBlank()) {
            return AppResult.Failure(AppError.ValidationFailed("content is required"))
        }
        return try {
            val post = communityPostRepository.createCommunityPost(
                userId = currentUser.id,
                title = title.trim(),
                content = content.trim(),
                imageUrls = imageUrls
            )
            communityPostEventPublisher.publish(CommunityPostEvent.Created(post.id))
            AppResult.Success(post)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(cause = e.message))
        }
    }
}
