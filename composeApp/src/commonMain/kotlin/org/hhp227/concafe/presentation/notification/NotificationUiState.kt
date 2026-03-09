package org.hhp227.concafe.presentation.notification

import org.hhp227.concafe.domain.model.NotificationSection

data class NotificationUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val unreadCount: Int = 0,
    val sections: List<NotificationSection> = emptyList()
) {
    companion object {
        fun empty(): NotificationUiState = NotificationUiState()
    }
}
