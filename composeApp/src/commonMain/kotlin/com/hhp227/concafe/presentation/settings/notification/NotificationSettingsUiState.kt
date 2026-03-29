package com.hhp227.concafe.presentation.settings.notification

import com.hhp227.concafe.domain.model.NotificationQuietHoursMode

data class NotificationSettingsUiState(
    val isLoading: Boolean,
    val isSaving: Boolean,
    val errorMessage: String?,
    val isCastRole: Boolean,
    val isPushNotificationsEnabled: Boolean,
    val isShiftNotificationsEnabled: Boolean,
    val isBirthdayNotificationsEnabled: Boolean,
    val isNoticeNotificationsEnabled: Boolean,
    val isFollowNotificationsEnabled: Boolean,
    val isEventNotificationsEnabled: Boolean,
    val quietHoursOption: NotificationQuietHoursMode
) {
    companion object {
        fun initial(): NotificationSettingsUiState {
            return NotificationSettingsUiState(
                isLoading = false,
                isSaving = false,
                errorMessage = null,
                isCastRole = false,
                isPushNotificationsEnabled = true,
                isShiftNotificationsEnabled = true,
                isBirthdayNotificationsEnabled = true,
                isNoticeNotificationsEnabled = false,
                isFollowNotificationsEnabled = true,
                isEventNotificationsEnabled = true,
                quietHoursOption = NotificationQuietHoursMode.NIGHT
            )
        }
    }
}
