package com.hhp227.concafe.presentation.settings.notification

data class NotificationSettingsUiState(
    val isPushNotificationsEnabled: Boolean,
    val isShiftNotificationsEnabled: Boolean,
    val isBirthdayNotificationsEnabled: Boolean,
    val isNoticeNotificationsEnabled: Boolean,
    val quietHoursOption: NotificationQuietHoursOption
) {
    companion object {
        fun initial(): NotificationSettingsUiState {
            return NotificationSettingsUiState(
                isPushNotificationsEnabled = true,
                isShiftNotificationsEnabled = true,
                isBirthdayNotificationsEnabled = true,
                isNoticeNotificationsEnabled = false,
                quietHoursOption = NotificationQuietHoursOption.NIGHT
            )
        }
    }
}
