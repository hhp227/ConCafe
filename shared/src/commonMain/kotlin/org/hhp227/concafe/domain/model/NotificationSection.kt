package com.hhp227.concafe.domain.model

data class NotificationSection(
    val id: String,
    val title: String,
    val items: List<NotificationListItem>
)
