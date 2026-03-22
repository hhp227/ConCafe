package com.hhp227.concafe.data.source.firestore

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSNumber

class IosFirebaseAuthSessionStore : FirebaseAuthSessionStore {
    override fun load(): FirebaseAuthSession? {
        val defaults = NSUserDefaults.standardUserDefaults
        val userId = defaults.stringForKey(KEY_USER_ID)
        val email = defaults.stringForKey(KEY_EMAIL)
        val idToken = defaults.stringForKey(KEY_ID_TOKEN)
        val refreshToken = defaults.stringForKey(KEY_REFRESH_TOKEN)
        val expiresAtEpochSeconds = (defaults.objectForKey(KEY_EXPIRES_AT_EPOCH_SECONDS) as? NSNumber)?.longLongValue
        return if (userId.isNullOrBlank() || email.isNullOrBlank()) {
            null
        } else {
            FirebaseAuthSession(
                userId = userId,
                email = email,
                idToken = idToken,
                refreshToken = refreshToken,
                expiresAtEpochSeconds = expiresAtEpochSeconds
            )
        }
    }

    override fun save(session: FirebaseAuthSession) {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.setObject(session.userId, forKey = KEY_USER_ID)
        defaults.setObject(session.email, forKey = KEY_EMAIL)
        defaults.setObject(session.idToken, forKey = KEY_ID_TOKEN)
        defaults.setObject(session.refreshToken, forKey = KEY_REFRESH_TOKEN)
        if (session.expiresAtEpochSeconds == null) {
            defaults.removeObjectForKey(KEY_EXPIRES_AT_EPOCH_SECONDS)
        } else {
            defaults.setObject(session.expiresAtEpochSeconds, forKey = KEY_EXPIRES_AT_EPOCH_SECONDS)
        }
        defaults.synchronize()
    }

    override fun clear() {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_EMAIL)
        defaults.removeObjectForKey(KEY_ID_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.removeObjectForKey(KEY_EXPIRES_AT_EPOCH_SECONDS)
        defaults.synchronize()
    }
}

private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
private const val KEY_REFRESH_TOKEN = "concafe.firebase.refresh_token"
private const val KEY_EXPIRES_AT_EPOCH_SECONDS = "concafe.firebase.expires_at_epoch_seconds"
