package com.hhp227.concafe.presentation.notification

sealed interface NotificationEvent {
    data object NavigateBack : NotificationEvent

    data class NavigateToCafe(val id: String) : NotificationEvent

    data class NavigateToCast(val id: String) : NotificationEvent

    data class NavigateToPost(val id: String) : NotificationEvent

    data object NavigateToSignIn : NotificationEvent
}
