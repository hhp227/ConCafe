package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.model.UserNotificationSettings
import com.hhp227.concafe.domain.repository.NotificationRepository

class FakeNotificationRepository(
    private val dataSource: ConCafeDataSource
) : NotificationRepository {
    private val settingsByUserId = mutableMapOf<String, UserNotificationSettings>()

    override suspend fun getNotifications(userId: String, cursor: String?, pageSize: Int): PagedResult<AppNotification> {
        val items = dataSource.notifications.filter { it.userId == userId }.sortedByDescending { it.createdAt }
        return dataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun markAsRead(userId: String, notificationId: String) {
        val index = dataSource.notifications.indexOfFirst { it.userId == userId && it.id == notificationId }
        if (index == -1) {
            throw NoSuchElementException("notification not found")
        }

        val current = dataSource.notifications[index]
        dataSource.notifications[index] = current.copy(isRead = true)
    }

    override suspend fun getNotificationSettings(userId: String): UserNotificationSettings {
        val cached = settingsByUserId[userId]

        if (cached != null) {
            return cached
        } else {
            return UserNotificationSettings.default()
        }
    }

    override suspend fun updateNotificationSettings(
        userId: String,
        settings: UserNotificationSettings
    ): UserNotificationSettings {
        settingsByUserId[userId] = settings
        return settings
    }

    override suspend fun registerPushToken(userId: String, platform: String, token: String) {
        if (userId.isBlank() || platform.isBlank() || token.isBlank()) {
            throw IllegalArgumentException("invalid push token payload")
        }
    }
}
