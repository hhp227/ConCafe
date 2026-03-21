package com.hhp227.concafe.data.source.firestore

import java.util.prefs.Preferences

class JvmFirebaseAuthSessionStore : FirebaseAuthSessionStore {
    private val preferences: Preferences = Preferences.userRoot().node(PREF_NODE)

    override fun load(): FirebaseAuthSession? {
        val userId = preferences.get(KEY_USER_ID, null)
        val email = preferences.get(KEY_EMAIL, null)
        val idToken = preferences.get(KEY_ID_TOKEN, null)
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
        preferences.put(KEY_USER_ID, session.userId)
        preferences.put(KEY_EMAIL, session.email)

        if (session.idToken == null) {
            preferences.remove(KEY_ID_TOKEN)
        } else {
            preferences.put(KEY_ID_TOKEN, session.idToken)
        }
    }

    override fun clear() {
        preferences.remove(KEY_USER_ID)
        preferences.remove(KEY_EMAIL)
        preferences.remove(KEY_ID_TOKEN)
    }
}

private const val PREF_NODE = "com.hhp227.concafe.firebase.auth"
private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
