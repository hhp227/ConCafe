package com.hhp227.concafe.data.source.firestore

import com.hhp227.concafe.domain.model.AuthProvider
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSNumber

class IosFirebaseAuthSessionStore : FirebaseAuthSessionStore {
    override fun load(): FirebaseAuthSession? {
        val defaults = NSUserDefaults.standardUserDefaults
        val userId = defaults.stringForKey(KEY_USER_ID)
        val email = defaults.stringForKey(KEY_EMAIL)
        val displayName = defaults.stringForKey(KEY_DISPLAY_NAME)
        val authProvider = defaults.stringForKey(KEY_AUTH_PROVIDER)
            ?.let { value -> runCatching { AuthProvider.valueOf(value) }.getOrDefault(AuthProvider.UNKNOWN) }
            ?: AuthProvider.UNKNOWN
        val idToken = defaults.stringForKey(KEY_ID_TOKEN)
        val refreshToken = defaults.stringForKey(KEY_REFRESH_TOKEN)
        val expiresAtEpochSeconds = (defaults.objectForKey(KEY_EXPIRES_AT_EPOCH_SECONDS) as? NSNumber)?.longLongValue
        val signupCompleted = (defaults.objectForKey(KEY_SIGNUP_COMPLETED) as? NSNumber)?.boolValue
        return if (userId.isNullOrBlank() || email.isNullOrBlank()) {
            null
        } else {
            FirebaseAuthSession(
                userId = userId,
                email = email,
                displayName = displayName,
                authProvider = authProvider,
                idToken = idToken,
                refreshToken = refreshToken,
                expiresAtEpochSeconds = expiresAtEpochSeconds,
                signupCompleted = signupCompleted
            )
        }
    }

    override fun save(session: FirebaseAuthSession) {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.setObject(session.userId, forKey = KEY_USER_ID)
        defaults.setObject(session.email, forKey = KEY_EMAIL)
        defaults.setObject(session.displayName, forKey = KEY_DISPLAY_NAME)
        defaults.setObject(session.authProvider.name, forKey = KEY_AUTH_PROVIDER)
        defaults.setObject(session.idToken, forKey = KEY_ID_TOKEN)
        defaults.setObject(session.refreshToken, forKey = KEY_REFRESH_TOKEN)
        if (session.expiresAtEpochSeconds == null) {
            defaults.removeObjectForKey(KEY_EXPIRES_AT_EPOCH_SECONDS)
        } else {
            defaults.setObject(session.expiresAtEpochSeconds, forKey = KEY_EXPIRES_AT_EPOCH_SECONDS)
        }
        if (session.signupCompleted == null) {
            defaults.removeObjectForKey(KEY_SIGNUP_COMPLETED)
        } else {
            defaults.setBool(session.signupCompleted, forKey = KEY_SIGNUP_COMPLETED)
        }
        defaults.synchronize()
    }

    override fun clear() {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_EMAIL)
        defaults.removeObjectForKey(KEY_DISPLAY_NAME)
        defaults.removeObjectForKey(KEY_AUTH_PROVIDER)
        defaults.removeObjectForKey(KEY_ID_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.removeObjectForKey(KEY_EXPIRES_AT_EPOCH_SECONDS)
        defaults.removeObjectForKey(KEY_SIGNUP_COMPLETED)
        defaults.synchronize()
    }
}

private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_DISPLAY_NAME = "concafe.firebase.display_name"
private const val KEY_AUTH_PROVIDER = "concafe.firebase.auth_provider"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
private const val KEY_REFRESH_TOKEN = "concafe.firebase.refresh_token"
private const val KEY_EXPIRES_AT_EPOCH_SECONDS = "concafe.firebase.expires_at_epoch_seconds"
private const val KEY_SIGNUP_COMPLETED = "concafe.firebase.signup_completed"
