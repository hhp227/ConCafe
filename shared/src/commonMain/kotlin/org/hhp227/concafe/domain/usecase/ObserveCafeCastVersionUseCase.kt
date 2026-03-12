package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.WatchHandle

class ObserveCafeCastVersionUseCase(
    private val castRepository: CastRepository
) {
    operator fun invoke(cafeId: String): Flow<Int> {
        return castRepository.observeCafeCastVersion(cafeId)
    }
    
    fun watch(cafeId: String, block: (Int) -> Unit): WatchHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            invoke(cafeId).collectLatest {
                block(it)
            }
        }
        return WatchHandle {
            job.cancel()
            scope.cancel()
        }
    }
}
