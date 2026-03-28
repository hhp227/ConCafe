package com.hhp227.concafe.push

import com.google.firebase.messaging.FirebaseMessagingService

class ConCafeFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        resolveAndroidPushTokenClient().saveToken(token = token)
    }
}
