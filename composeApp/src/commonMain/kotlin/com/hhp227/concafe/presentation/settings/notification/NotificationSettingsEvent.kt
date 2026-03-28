package com.hhp227.concafe.presentation.settings.notification

sealed interface NotificationSettingsEvent {
    data object NavigateBack : NotificationSettingsEvent
    data class ShowMessage(val message: String) : NotificationSettingsEvent
}
