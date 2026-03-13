package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.WatchHandle
import com.hhp227.concafe.domain.model.ReviewEvent
import com.hhp227.concafe.domain.repository.ReviewRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ObserveReviewEventUseCase(
    private val reviewRepository: ReviewRepository
) {
    operator fun invoke(): Flow<ReviewEvent> {
        return reviewRepository.observeReviewEvent()
    }

    fun watch(block: (ReviewEvent) -> Unit): WatchHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            invoke().collectLatest { event ->
                block(event)
            }
        }
        return WatchHandle {
            job.cancel()
            scope.cancel()
        }
    }
}
