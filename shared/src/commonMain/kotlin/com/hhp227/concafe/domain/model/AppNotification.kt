package com.hhp227.concafe.domain.model

data class AppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val targetId: String?,
    val isRead: Boolean,
    val createdAt: String,
    val relativeTime: String
)
