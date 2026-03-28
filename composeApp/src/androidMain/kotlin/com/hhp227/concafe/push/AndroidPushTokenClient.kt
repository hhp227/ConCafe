package com.hhp227.concafe.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.core.context.GlobalContext
import androidx.core.content.edit

private const val PREF_NAME = "concafe.push.token"

private const val KEY_FCM_TOKEN = "fcm_token"

fun resolveAndroidPushTokenClient(): AndroidPushTokenClient {
    val koin = checkNotNull(GlobalContext.getOrNull()) {
        "Koin is not initialized. Call doInitConCafeAppKoin() before resolving dependencies."
    }
    return koin.get()
}

class AndroidPushTokenClient(
    private val context: Context,
    private val firebaseMessaging: FirebaseMessaging
) {
    fun requestAndStoreToken(onToken: (String) -> Unit) {
        firebaseMessaging.token.addOnSuccessListener { token ->
            saveToken(token = token)
            onToken(token)
        }
    }

    fun saveToken(token: String) {
        val normalizedToken = token.trim()

        if (normalizedToken.isEmpty()) {
            return
        } else {
            val sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit { putString(KEY_FCM_TOKEN, normalizedToken) }
        }
    }

    fun currentStoredToken(): String {
        val sharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(KEY_FCM_TOKEN, "") ?: ""
        return token.trim()
    }
}
