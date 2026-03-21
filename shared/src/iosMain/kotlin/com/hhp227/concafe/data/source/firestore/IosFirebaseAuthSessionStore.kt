package com.hhp227.concafe.data.source.firestore

import platform.Foundation.NSUserDefaults

class IosFirebaseAuthSessionStore : FirebaseAuthSessionStore {
    override fun load(): FirebaseAuthSession? {
        val defaults = NSUserDefaults.standardUserDefaults
        val userId = defaults.stringForKey(KEY_USER_ID)
        val email = defaults.stringForKey(KEY_EMAIL)
        val idToken = defaults.stringForKey(KEY_ID_TOKEN)
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
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.setObject(session.userId, forKey = KEY_USER_ID)
        defaults.setObject(session.email, forKey = KEY_EMAIL)
        defaults.setObject(session.idToken, forKey = KEY_ID_TOKEN)
        defaults.synchronize()
    }

    override fun clear() {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.removeObjectForKey(KEY_USER_ID)
        defaults.removeObjectForKey(KEY_EMAIL)
        defaults.removeObjectForKey(KEY_ID_TOKEN)
        defaults.synchronize()
    }
}

private const val KEY_USER_ID = "concafe.firebase.user_id"
private const val KEY_EMAIL = "concafe.firebase.email"
private const val KEY_ID_TOKEN = "concafe.firebase.id_token"
