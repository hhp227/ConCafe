package com.hhp227.concafe.data.source.firestore

import java.util.prefs.Preferences

class JvmFirestoreRestCacheStore : FirestoreRestCacheStore {
    private val preferences: Preferences = Preferences.userRoot().node(PREF_NODE)

    override fun load(key: String): String? {
        return preferences.get(resolveStorageKey(key), null)
    }

    override fun save(key: String, payload: String) {
        preferences.put(resolveStorageKey(key), payload)
    }

    override fun clear() {
        preferences.keys()
            .filter { storedKey -> storedKey.startsWith(KEY_PREFIX) }
            .forEach { storedKey ->
                preferences.remove(storedKey)
            }
    }

    private fun resolveStorageKey(key: String): String {
        return "$KEY_PREFIX${key.hashCode()}"
    }
}

private const val PREF_NODE = "com.hhp227.concafe.firestore.http.cache"
private const val KEY_PREFIX = "concafe.firestore.http.cache.entry."
