package com.hhp227.concafe.data.source.firestore

interface FirestoreRestApi {
    suspend fun get(path: String, idToken: String? = null): String

    suspend fun post(path: String, body: String, idToken: String? = null): String

    suspend fun patch(path: String, body: String, idToken: String? = null, updateMask: List<String> = emptyList()): String

    suspend fun delete(path: String, idToken: String? = null)
}