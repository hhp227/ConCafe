package org.hhp227.concafe.domain.model

data class NotificationListItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val targetId: String?,
    val isRead: Boolean,
    val relativeTime: String
)
