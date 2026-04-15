package com.hhp227.concafe.data.source.firestore

import android.content.Context
import com.hhp227.concafe.domain.model.AuthProvider

class AndroidFirebaseAuthSessionStore(
    context: Context
) : FirebaseAuthSessionStore {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): FirebaseAuthSession? {
        val userId = sharedPreferences.getString(KEY_USER_ID, null)
        val email = sharedPreferences.getString(KEY_EMAIL, null)
        val displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, null)
        val authProvider = sharedPreferences.getString(KEY_AUTH_PROVIDER, null)
            ?.let { value -> runCatching { AuthProvider.valueOf(value) }.getOrDefault(AuthProvider.UNKNOWN) }
            ?: AuthProvider.UNKNOWN
        val idToken = sharedPreferences.getString(KEY_ID_TOKEN, null)
        val refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
        val expiresAtEpochSeconds = if (sharedPreferences.contains(KEY_EXPIRES_AT_EPOCH_SECONDS)) {
            sharedPreferences.getLong(KEY_EXPIRES_AT_EPOCH_SECONDS, 0L)
        } else {
            null
        }
        val signupCompleted = if (sharedPreferences.contains(KEY_SIGNUP_COMPLETED)) {
            sharedPreferences.getBoolean(KEY_SIGNUP_COMPLETED, false)
        } else {
            null
        }
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
        sharedPreferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_AUTH_PROVIDER, session.authProvider.name)
            .putString(KEY_ID_TOKEN, session.idToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply {
                if (session.expiresAtEpochSeconds == null) {
                    remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
                } else {
                    putLong(KEY_EXPIRES_AT_EPOCH_SECONDS, session.expiresAtEpochSeconds)
                }
                if (session.signupCompleted == null) {
                    remove(KEY_SIGNUP_COMPLETED)
                } else {
                    putBoolean(KEY_SIGNUP_COMPLETED, session.signupCompleted)
                }
            }
            .apply()
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_AUTH_PROVIDER)
            .remove(KEY_ID_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT_EPOCH_SECONDS)
            .remove(KEY_SIGNUP_COMPLETED)
            .apply()
    }
}

private const val PREFS_NAME = "concafe.firebase.auth"
private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_DISPLAY_NAME = "concafe.firebase.display_name"
private const val KEY_AUTH_PROVIDER = "concafe.firebase.auth_provider"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
private const val KEY_REFRESH_TOKEN = "concafe.firebase.refresh_token"
private const val KEY_EXPIRES_AT_EPOCH_SECONDS = "concafe.firebase.expires_at_epoch_seconds"
private const val KEY_SIGNUP_COMPLETED = "concafe.firebase.signup_completed"
