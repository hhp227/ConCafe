package com.hhp227.concafe.data.source.firestore

import java.util.prefs.Preferences

class JvmFirebaseAuthSessionStore : FirebaseAuthSessionStore {
    private val preferences: Preferences = Preferences.userRoot().node(PREF_NODE)

    override fun load(): FirebaseAuthSession? {
        val userId = preferences.get(KEY_USER_ID, null)
        val email = preferences.get(KEY_EMAIL, null)
        val idToken = preferences.get(KEY_ID_TOKEN, null)
        val refreshToken = preferences.get(KEY_REFRESH_TOKEN, null)
        val expiresAtEpochSeconds = if (preferences.get(KEY_EXPIRES_AT_EPOCH_SECONDS, null) == null) {
            null
        } else {
            preferences.getLong(KEY_EXPIRES_AT_EPOCH_SECONDS, 0L)
        }
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
        preferences.put(KEY_USER_ID, session.userId)
        preferences.put(KEY_EMAIL, session.email)

        if (session.idToken == null) {
            preferences.remove(KEY_ID_TOKEN)
        } else {
            preferences.put(KEY_ID_TOKEN, session.idToken)
        }
        if (session.refreshToken == null) {
            preferences.remove(KEY_REFRESH_TOKEN)
        } else {
            preferences.put(KEY_REFRESH_TOKEN, session.refreshToken)
        }
        if (session.expiresAtEpochSeconds == null) {
            preferences.remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
        } else {
            preferences.putLong(KEY_EXPIRES_AT_EPOCH_SECONDS, session.expiresAtEpochSeconds)
        }
    }

    override fun clear() {
        preferences.remove(KEY_USER_ID)
        preferences.remove(KEY_EMAIL)
        preferences.remove(KEY_ID_TOKEN)
        preferences.remove(KEY_REFRESH_TOKEN)
        preferences.remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
    }
}

private const val PREF_NODE = "com.hhp227.concafe.firebase.auth"
private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
private const val KEY_REFRESH_TOKEN = "concafe.firebase.refresh_token"
private const val KEY_EXPIRES_AT_EPOCH_SECONDS = "concafe.firebase.expires_at_epoch_seconds"
