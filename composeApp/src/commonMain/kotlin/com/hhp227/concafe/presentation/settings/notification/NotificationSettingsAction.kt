package com.hhp227.concafe.presentation.settings.notification

import com.hhp227.concafe.domain.model.NotificationQuietHoursMode

sealed interface NotificationSettingsAction {
    data object ClickBack : NotificationSettingsAction
    data class TogglePushNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleShiftNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleBirthdayNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleNoticeNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleFollowNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class ToggleEventNotifications(val enabled: Boolean) : NotificationSettingsAction
    data class SelectQuietHours(val option: NotificationQuietHoursMode) : NotificationSettingsAction
}
