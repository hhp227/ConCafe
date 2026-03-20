package com.hhp227.concafe.data.source.firestore

interface FirestoreRestApi {
    suspend fun get(path: String, idToken: String? = null): String

    suspend fun post(path: String, body: String, idToken: String? = null): String

    suspend fun patch(path: String, body: String, idToken: String? = null): String

    suspend fun delete(path: String, idToken: String? = null)
}

class NoOpFirestoreRestApi : FirestoreRestApi {
    override suspend fun get(path: String, idToken: String?): String {
        throw UnsupportedOperationException("Firestore REST client is not configured.")
    }

    override suspend fun post(path: String, body: String, idToken: String?): String {
        throw UnsupportedOperationException("Firestore REST client is not configured.")
    }

    override suspend fun patch(path: String, body: String, idToken: String?): String {
        throw UnsupportedOperationException("Firestore REST client is not configured.")
    }

    override suspend fun delete(path: String, idToken: String?) {
        throw UnsupportedOperationException("Firestore REST client is not configured.")
    }
}

