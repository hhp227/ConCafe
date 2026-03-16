package com.hhp227.concafe.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.WatchHandle
import com.hhp227.concafe.domain.repository.CastRepository

class ObserveCastVersionUseCase(
    private val castRepository: CastRepository
) {
    operator fun invoke(castId: String): Flow<Int> {
        return castRepository.observeCastVersion(castId)
    }

    fun watch(castId: String, block: (Int) -> Unit): WatchHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            invoke(castId).collectLatest {
                block(it)
            }
        }
        return WatchHandle {
            job.cancel()
            scope.cancel()
        }
    }
}
