package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.UserNotificationSettings
import com.hhp227.concafe.domain.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val notificationDataSource: NotificationDataSource
) : NotificationRepository {
    override suspend fun getNotifications(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<AppNotification> {
        return notificationDataSource.getNotifications(
            userId = userId,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun markAsRead(userId: String, notificationId: String) {
        notificationDataSource.markNotificationAsRead(
            userId = userId,
            notificationId = notificationId
        )
    }

    override suspend fun getNotificationSettings(userId: String): UserNotificationSettings {
        return notificationDataSource.getNotificationSettings(userId)
    }

    override suspend fun updateNotificationSettings(
        userId: String,
        settings: UserNotificationSettings
    ): UserNotificationSettings {
        return notificationDataSource.updateNotificationSettings(userId, settings)
    }

    override suspend fun registerPushToken(userId: String, platform: String, token: String) {
        notificationDataSource.registerPushToken(
            userId = userId,
            platform = platform,
            token = token
        )
    }
}
