package com.hhp227.concafe.data.source.firestore

import android.content.Context

class AndroidFirebaseAuthSessionStore(
    context: Context
) : FirebaseAuthSessionStore {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): FirebaseAuthSession? {
        val userId = sharedPreferences.getString(KEY_USER_ID, null)
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val idToken = sharedPreferences.getString(KEY_ID_TOKEN, null)
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        val expiresAtEpochSeconds = if (sharedPreferences.contains(KEY_EXPIRES_AT_EPOCH_SECONDS)) {
            sharedPreferences.getLong(KEY_EXPIRES_AT_EPOCH_SECONDS, 0L)
        } else {
            null
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
        sharedPreferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_ID_TOKEN, session.idToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply {
                if (session.expiresAtEpochSeconds == null) {
                    remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
                } else {
                    putLong(KEY_EXPIRES_AT_EPOCH_SECONDS, session.expiresAtEpochSeconds)
                }
            }
            .apply()
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_ID_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
            .apply()
    }
}

private const val PREFS_NAME = "concafe.firebase.auth"
private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
private const val KEY_REFRESH_TOKEN = "concafe.firebase.refresh_token"
private const val KEY_EXPIRES_AT_EPOCH_SECONDS = "concafe.firebase.expires_at_epoch_seconds"
