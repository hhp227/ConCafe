package com.hhp227.concafe.presentation.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationSettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationSettingsUiState.initial())
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<NotificationSettingsEvent>(replay = 0)
    val event = _event.asSharedFlow()

    private fun clickBack() {
        viewModelScope.launch {
            _event.emit(NotificationSettingsEvent.NavigateBack)
        }
    }

    private fun togglePushNotifications(enabled: Boolean) {
        _uiState.update { it.copy(isPushNotificationsEnabled = enabled) }
    }

    private fun toggleShiftNotifications(enabled: Boolean) {
        _uiState.update { it.copy(isShiftNotificationsEnabled = enabled) }
    }

    private fun toggleBirthdayNotifications(enabled: Boolean) {
        _uiState.update { it.copy(isBirthdayNotificationsEnabled = enabled) }
    }

    private fun toggleNoticeNotifications(enabled: Boolean) {
        _uiState.update { it.copy(isNoticeNotificationsEnabled = enabled) }
    }

    private fun selectQuietHours(option: NotificationQuietHoursOption) {
        _uiState.update { it.copy(quietHoursOption = option) }
    }

    fun onAction(action: NotificationSettingsAction) {
        when (action) {
            NotificationSettingsAction.ClickBack -> clickBack()
            is NotificationSettingsAction.TogglePushNotifications -> {
                togglePushNotifications(action.enabled)
            }
            is NotificationSettingsAction.ToggleShiftNotifications -> {
                toggleShiftNotifications(action.enabled)
            }
            is NotificationSettingsAction.ToggleBirthdayNotifications -> {
                toggleBirthdayNotifications(action.enabled)
            }
            is NotificationSettingsAction.ToggleNoticeNotifications -> {
                toggleNoticeNotifications(action.enabled)
            }
            is NotificationSettingsAction.SelectQuietHours -> selectQuietHours(action.option)
        }
    }
}
