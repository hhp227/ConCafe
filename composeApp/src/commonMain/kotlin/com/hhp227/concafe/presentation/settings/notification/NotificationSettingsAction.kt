package com.hhp227.concafe.presentation.settings.notification

sealed interface NotificationSettingsAction {
    data object ClickBack : NotificationSettingsAction
    data class TogglePushNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleShiftNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleBirthdayNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleNoticeNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class SelectQuietHours(val option: NotificationQuietHoursOption) : NotificationSettingsAction
}
