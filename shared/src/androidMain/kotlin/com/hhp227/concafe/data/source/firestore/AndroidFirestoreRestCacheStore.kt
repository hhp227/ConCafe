package com.hhp227.concafe.data.source.firestore

import android.content.Context

class AndroidFirestoreRestCacheStore(
    context: Context
) : FirestoreRestCacheStore {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(key: String): String? {
        return sharedPreferences.getString(resolveStorageKey(key), null)
    }

    override fun save(key: String, payload: String) {
        sharedPreferences.edit()
            .putString(resolveStorageKey(key), payload)
            .apply()
    }

    override fun clear() {
        val editor = sharedPreferences.edit()

        sharedPreferences.all.keys
            .filter { storedKey -> storedKey.startsWith(KEY_PREFIX) }
            .forEach { storedKey ->
                editor.remove(storedKey)
            }
        editor.apply()
    }

    private fun resolveStorageKey(key: String): String {
        return "$KEY_PREFIX${key.hashCode()}"
    }
}

private const val PREFS_NAME = "concafe.firestore.http.cache"
private const val KEY_PREFIX = "concafe.firestore.http.cache.entry."
