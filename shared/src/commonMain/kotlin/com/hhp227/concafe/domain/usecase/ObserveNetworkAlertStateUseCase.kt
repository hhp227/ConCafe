package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.NetworkAlertState
import com.hhp227.concafe.domain.repository.NetworkStatusRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class ObserveNetworkAlertStateUseCase(
    private val networkStatusRepository: NetworkStatusRepository
) {
    private val recoveredMessageDurationMillis = 1_800L

    operator fun invoke(): Flow<NetworkAlertState> {
        return flow {
            var previousIsConnected: Boolean? = null

            networkStatusRepository.observeIsConnected()
                .distinctUntilChanged()
                .collect { isConnected ->
                    if (isConnected) {
                        if (previousIsConnected == false) {
                            emit(NetworkAlertState.recovered)
                            delay(recoveredMessageDurationMillis)
                            emit(NetworkAlertState.hidden)
                        } else {
                            emit(NetworkAlertState.hidden)
                        }
                    } else {
                        emit(NetworkAlertState.offline)
                    }
                    previousIsConnected = isConnected
                }
        }
    }
}
