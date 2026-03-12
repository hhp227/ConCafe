package com.hhp227.concafe.domain.model

data class NotificationFeed(
    val isLoggedIn: Boolean,
    val unreadCount: Int,
    val sections: List<NotificationSection>
)
