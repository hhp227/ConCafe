package com.hhp227.concafe.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.hhp227.concafe.domain.common.WatchHandle
import com.hhp227.concafe.domain.model.CafeDetail
import com.hhp227.concafe.domain.repository.CafeRepository

class ObserveCafeDetailUseCase(
    private val cafeRepository: CafeRepository
) {
    operator fun invoke(cafeId: String): Flow<CafeDetail> {
        return cafeRepository.observeCafeDetail(cafeId)
    }

    fun watch(cafeId: String, block: (CafeDetail) -> Unit): WatchHandle {
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
