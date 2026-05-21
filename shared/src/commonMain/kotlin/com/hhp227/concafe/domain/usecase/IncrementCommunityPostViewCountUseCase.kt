package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.repository.CommunityPostRepository

class IncrementCommunityPostViewCountUseCase(
    private val communityPostRepository: CommunityPostRepository
) {
    suspend operator fun invoke(postId: String) {
        if (postId.isBlank()) return
        runCatching { communityPostRepository.incrementViewCount(postId) }
    }
}
