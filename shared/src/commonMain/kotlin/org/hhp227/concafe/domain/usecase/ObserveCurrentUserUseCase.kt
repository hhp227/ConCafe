package org.hhp227.concafe.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.hhp227.concafe.domain.common.WatchHandle
import kotlinx.coroutines.flow.Flow
import org.hhp227.concafe.domain.model.User
import org.hhp227.concafe.domain.repository.AuthRepository

class ObserveCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return authRepository.observeCurrentUser()
    }

    fun watch(block: (User?) -> Unit): WatchHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val job = scope.launch {
            invoke().collectLatest {
                block(it)
            }
        }
        return WatchHandle {
            job.cancel()
            scope.cancel()
        }
    }
}
