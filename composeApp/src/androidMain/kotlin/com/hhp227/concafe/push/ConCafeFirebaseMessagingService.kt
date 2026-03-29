package com.hhp227.concafe.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hhp227.concafe.R
import com.hhp227.concafe.di.resolveRegisterPushTokenUseCase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConCafeFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val notification = remoteMessage.notification
        val data = remoteMessage.data
        val title = notification?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = notification?.body ?: data["body"] ?: ""
        val notificationId = (data["notificationId"] ?: remoteMessage.messageId ?: System.currentTimeMillis().toString())
            .hashCode()

        if (body.isEmpty()) {
            return
        } else {
            ensureDefaultChannel()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)

                if (permission != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            val builder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            NotificationManagerCompat.from(this).notify(notificationId, builder.build())
        }
    }

    override fun onNewToken(token: String) {
        resolveAndroidPushTokenClient().saveToken(token = token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                resolveRegisterPushTokenUseCase().invoke(
                    platform = "ANDROID",
                    token = token
                )
            }
        }
    }

    private fun ensureDefaultChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        } else {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val existing = manager.getNotificationChannel(DEFAULT_CHANNEL_ID)

            if (existing == null) {
                val channel = NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    getString(R.string.push_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )

                channel.description = getString(R.string.push_channel_description)
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val DEFAULT_CHANNEL_ID = "concafe_default"
    }
}
