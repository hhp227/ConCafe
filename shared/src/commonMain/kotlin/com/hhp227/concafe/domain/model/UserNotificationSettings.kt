package com.hhp227.concafe.domain.model

data class UserNotificationSettings(
    val isPushNotificationsEnabled: Boolean,
    val isShiftNotificationsEnabled: Boolean,
    val isBirthdayNotificationsEnabled: Boolean,
    val isNoticeNotificationsEnabled: Boolean,
    val quietHoursMode: NotificationQuietHoursMode
) {
    companion object {
        fun default(): UserNotificationSettings {
            return UserNotificationSettings(
                isPushNotificationsEnabled = true,
                isShiftNotificationsEnabled = true,
                isBirthdayNotificationsEnabled = true,
                isNoticeNotificationsEnabled = false,
                quietHoursMode = NotificationQuietHoursMode.NIGHT
            )
        }
    }
}

enum class NotificationQuietHoursMode {
    OFF,
    NIGHT,
    ALL_DAY
}
