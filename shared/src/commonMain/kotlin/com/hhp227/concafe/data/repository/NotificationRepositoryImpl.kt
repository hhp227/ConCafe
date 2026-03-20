package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.NotificationDataSource
import com.hhp227.concafe.data.source.PagingDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification
import com.hhp227.concafe.domain.repository.NotificationRepository

class NotificationRepositoryImpl(
    private val notificationDataSource: NotificationDataSource,
    private val pagingDataSource: PagingDataSource
) : NotificationRepository {
    override suspend fun getNotifications(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<AppNotification> {
        val items = notificationDataSource.notifications
            .filter { it.userId == userId }
            .sortedByDescending { it.createdAt }
        return pagingDataSource.toPaged(items, cursor, pageSize)
    }

    override suspend fun markAsRead(userId: String, notificationId: String) {
        val index = notificationDataSource.notifications.indexOfFirst {
            it.userId == userId && it.id == notificationId
        }
        if (index == -1) {
            throw NoSuchElementException("notification not found")
        }

        val current = notificationDataSource.notifications[index]
        notificationDataSource.notifications[index] = current.copy(isRead = true)
    }
}
