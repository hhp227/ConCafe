package com.hhp227.concafe.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.hhp227.concafe.R

object PushNotificationChannels {
    const val DEFAULT_CHANNEL_ID = "concafe_default"

    fun ensureDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(DEFAULT_CHANNEL_ID)

        if (existing != null) {
            return
        }
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            context.getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description = context.getString(R.string.push_channel_description)
        manager.createNotificationChannel(channel)
    }
}
