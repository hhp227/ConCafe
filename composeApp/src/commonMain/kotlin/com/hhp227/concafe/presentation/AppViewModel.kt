package com.hhp227.concafe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.ObserveNetworkAlertStateUseCase
import com.hhp227.concafe.domain.usecase.RegisterPushTokenUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<AppEvent>(replay = 0)
    val event: SharedFlow<AppEvent> = _event.asSharedFlow()

    private val jobs = mutableMapOf<JobKey, Job>()

    private fun observeNetworkAlertState() {
        jobs[JobKey.OBSERVE_NETWORK_ALERT]?.cancel()
        jobs[JobKey.OBSERVE_NETWORK_ALERT] = viewModelScope.launch {
            observeNetworkAlertStateUseCase.invoke().collect { networkAlertState ->
                _uiState.update {
                    it.copy(networkAlertState = networkAlertState)
                }
            }
        }
    }

    private fun observeSessionAndSyncPushToken() {
        jobs[JobKey.OBSERVE_CURRENT_USER]?.cancel()
        jobs[JobKey.OBSERVE_CURRENT_USER] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest {
                _event.emit(AppEvent.SyncPushToken)
            }
        }
    }

    private fun syncPushToken(token: String) {
        val normalizedToken = token.trim()

        if (normalizedToken.isEmpty()) {
            return
        } else {
            viewModelScope.launch {
                registerPushTokenUseCase.invoke(
                    platform = "ANDROID",
                    token = normalizedToken
                )
            }
        }
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.SyncPushToken -> syncPushToken(action.token)
        }
    }

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeNetworkAlertState()
        observeSessionAndSyncPushToken()
    }

    private enum class JobKey {
        OBSERVE_NETWORK_ALERT,
        OBSERVE_CURRENT_USER
    }
}
