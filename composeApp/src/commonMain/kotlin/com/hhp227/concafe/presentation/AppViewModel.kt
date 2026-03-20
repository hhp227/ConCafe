package com.hhp227.concafe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.di.resolveObserveNetworkAlertStateUseCase
import com.hhp227.concafe.domain.usecase.ObserveNetworkAlertStateUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase = resolveObserveNetworkAlertStateUseCase()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

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

    override fun onCleared() {
        jobs.values.forEach(Job::cancel)
        jobs.clear()
        super.onCleared()
    }

    init {
        observeNetworkAlertState()
    }

    private enum class JobKey {
        OBSERVE_NETWORK_ALERT
    }
}
