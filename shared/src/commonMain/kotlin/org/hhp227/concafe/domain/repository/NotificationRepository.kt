package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AppNotification

interface NotificationRepository {
    suspend fun getNotifications(userId: String, cursor: String?, pageSize: Int): PagedResult<AppNotification>

    suspend fun markAsRead(userId: String, notificationId: String)
}
