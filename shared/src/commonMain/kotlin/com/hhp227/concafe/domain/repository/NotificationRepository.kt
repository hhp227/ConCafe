package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.UserNotificationSettings

interface NotificationRepository {
    suspend fun getNotifications(userId: String, cursor: String?, pageSize: Int): PagedResult<AppNotification>

    suspend fun markAsRead(userId: String, notificationId: String)

    suspend fun getNotificationSettings(userId: String): UserNotificationSettings

    suspend fun updateNotificationSettings(userId: String, settings: UserNotificationSettings): UserNotificationSettings

    suspend fun registerPushToken(userId: String, platform: String, token: String)

    suspend fun disableAllPushTokens(userId: String)

    suspend fun sendFanAnnouncement(
        userId: String,
        cafeId: String,
        castId: String,
        title: String,
        body: String
    )
}
