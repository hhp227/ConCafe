package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.WatchHandle
import com.hhp227.concafe.domain.model.CastEvent
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ObserveCastEventUseCase(
    private val castRepository: CastRepository
) {
    operator fun invoke(): Flow<CastEvent> {
        return castRepository.observeCastEvent()
    }

    fun watch(block: (CastEvent) -> Unit): WatchHandle {
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
