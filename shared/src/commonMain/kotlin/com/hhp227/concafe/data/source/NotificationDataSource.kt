package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.UserNotificationSettings

interface NotificationDataSource {
    val notifications: MutableList<AppNotification>

    suspend fun getNotifications(userId: String, cursor: String?, pageSize: Int): PagedResult<AppNotification>

    suspend fun markNotificationAsRead(userId: String, notificationId: String)

    suspend fun getNotificationSettings(userId: String): UserNotificationSettings

    suspend fun updateNotificationSettings(userId: String, settings: UserNotificationSettings): UserNotificationSettings
}
