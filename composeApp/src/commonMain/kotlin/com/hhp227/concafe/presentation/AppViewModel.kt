package com.hhp227.concafe.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.usecase.GetNotificationFeedUseCase
import com.hhp227.concafe.domain.usecase.ObserveThemeModeUseCase
import com.hhp227.concafe.domain.usecase.ObserveCurrentUserUseCase
import com.hhp227.concafe.domain.usecase.ObserveNetworkAlertStateUseCase
import com.hhp227.concafe.domain.usecase.RegisterPushTokenUseCase
import com.hhp227.concafe.presentation.theme.toPresentationThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppViewModel(
    private val observeNetworkAlertStateUseCase: ObserveNetworkAlertStateUseCase,
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
    private val getNotificationFeedUseCase: GetNotificationFeedUseCase,
    private val observeThemeModeUseCase: ObserveThemeModeUseCase
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
                _uiState.update { it.copy(networkAlertState = networkAlertState) }
            }
        }
    }

    private fun observeThemeMode() {
        jobs[JobKey.OBSERVE_THEME]?.cancel()
        jobs[JobKey.OBSERVE_THEME] = viewModelScope.launch {
            observeThemeModeUseCase.invoke().collect { themeMode ->
                _uiState.update { it.copy(themeMode = themeMode.toPresentationThemeMode()) }
            }
        }
    }

    private fun observeSessionAndSyncPushToken() {
        jobs[JobKey.OBSERVE_CURRENT_USER]?.cancel()
        jobs[JobKey.OBSERVE_CURRENT_USER] = viewModelScope.launch {
            observeCurrentUserUseCase.invoke().collectLatest { user ->
                if (user != null) {
                    _event.emit(AppEvent.SyncPushToken)
                    startUnreadNotificationPolling()
                } else {
                    stopUnreadNotificationPolling()
                    _uiState.update { it.copy(hasUnreadNotifications = false) }
                }
            }
        }
    }

    private suspend fun refreshUnreadNotificationCount() {
        val result = getNotificationFeedUseCase.invoke()

        if (result is AppResult.Success) {
            val hasUnread = result.data.unreadCount > 0

            _uiState.update { current ->
                if (current.hasUnreadNotifications == hasUnread) current
                else current.copy(hasUnreadNotifications = hasUnread)
            }
        }
    }

    private fun startUnreadNotificationPolling() {
        jobs[JobKey.UNREAD_NOTIFICATION_POLL]?.cancel()
        jobs[JobKey.UNREAD_NOTIFICATION_POLL] = viewModelScope.launch {
            while (isActive) {
                refreshUnreadNotificationCount()
                delay(UNREAD_NOTIFICATION_POLL_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopUnreadNotificationPolling() {
        jobs[JobKey.UNREAD_NOTIFICATION_POLL]?.cancel()
        jobs.remove(JobKey.UNREAD_NOTIFICATION_POLL)
    }

    private fun syncPushToken(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) return
        viewModelScope.launch {
            registerPushTokenUseCase.invoke(platform = "ANDROID", token = normalizedToken)
        }
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.SyncPushToken -> syncPushToken(action.token)
            is AppAction.RefreshUnreadNotificationCount -> viewModelScope.launch {
                refreshUnreadNotificationCount()
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
        observeThemeMode()
        observeSessionAndSyncPushToken()
    }

    private enum class JobKey {
        OBSERVE_NETWORK_ALERT,
        OBSERVE_THEME,
        OBSERVE_CURRENT_USER,
        UNREAD_NOTIFICATION_POLL
    }
}

private const val UNREAD_NOTIFICATION_POLL_INTERVAL_MILLIS = 60_000L
