package com.hhp227.concafe.presentation.settings.notification

import com.hhp227.concafe.domain.model.NotificationQuietHoursMode

data class NotificationSettingsUiState(
    val isLoading: Boolean,
    val isSaving: Boolean,
    val errorMessage: String?,
    val isPushNotificationsEnabled: Boolean,
    val isShiftNotificationsEnabled: Boolean,
    val isBirthdayNotificationsEnabled: Boolean,
    val isNoticeNotificationsEnabled: Boolean,
    val quietHoursOption: NotificationQuietHoursMode
) {
    companion object {
        fun initial(): NotificationSettingsUiState {
            return NotificationSettingsUiState(
                isLoading = false,
                isSaving = false,
                errorMessage = null,
                isPushNotificationsEnabled = true,
                isShiftNotificationsEnabled = true,
                isBirthdayNotificationsEnabled = true,
                isNoticeNotificationsEnabled = false,
                quietHoursOption = NotificationQuietHoursMode.NIGHT
            )
        }
    }
}
