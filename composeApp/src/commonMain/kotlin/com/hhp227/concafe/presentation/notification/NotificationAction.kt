package com.hhp227.concafe.presentation.notification

sealed interface NotificationAction {
    data object ClickBack : NotificationAction

    data class ClickNotification(val id: String, val type: String, val targetId: String?) : NotificationAction

    data object ClickSignIn : NotificationAction

    data object Refresh : NotificationAction
}
