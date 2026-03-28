package com.hhp227.concafe.data.source.firestore

import platform.Foundation.NSUserDefaults

class IosFirestoreRestCacheStore : FirestoreRestCacheStore {
    override fun load(key: String): String? {
        val defaults = NSUserDefaults.standardUserDefaults

        return defaults.stringForKey(resolveStorageKey(key))
    }

    override fun save(key: String, payload: String) {
        val defaults = NSUserDefaults.standardUserDefaults

        defaults.setObject(payload, forKey = resolveStorageKey(key))
        defaults.synchronize()
    }

    override fun clear() {
        val defaults = NSUserDefaults.standardUserDefaults
        val dictionary = defaults.dictionaryRepresentation()
        val keys = dictionary.keys

        for (key in keys) {
            val keyString = key.toString()

            if (keyString.startsWith(KEY_PREFIX)) {
                defaults.removeObjectForKey(keyString)
            }
        }
        defaults.synchronize()
    }

    private fun resolveStorageKey(key: String): String {
        return "$KEY_PREFIX${key.hashCode()}"
    }
}

private const val KEY_PREFIX = "concafe.firestore.http.cache.entry."
