package com.hhp227.concafe.data.source.firestore

interface FirestoreRestCacheStore {
    fun load(key: String): String?

    fun save(key: String, payload: String)

    fun clear()
}
