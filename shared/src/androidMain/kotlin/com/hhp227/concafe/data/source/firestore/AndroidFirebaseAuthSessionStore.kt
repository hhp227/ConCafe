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
        return if (userId.isNullOrBlank() || email.isNullOrBlank()) {
            null
        } else {
            FirebaseAuthSession(
                userId = userId,
                email = email,
                idToken = idToken
            )
        }
    }

    override fun save(session: FirebaseAuthSession) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_ID_TOKEN, session.idToken)
            .apply()
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .remove(KEY_ID_TOKEN)
            .apply()
    }
}

private const val PREFS_NAME = "concafe.firebase.auth"
private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
