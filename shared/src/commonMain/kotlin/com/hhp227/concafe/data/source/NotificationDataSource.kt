package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.AppNotification

interface NotificationDataSource {
    val notifications: MutableList<AppNotification>
}