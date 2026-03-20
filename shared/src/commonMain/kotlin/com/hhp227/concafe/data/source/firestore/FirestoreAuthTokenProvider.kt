package com.hhp227.concafe.data.source.firestore

interface FirestoreAuthTokenProvider {
    suspend fun getIdToken(): String?
}

class NoOpFirestoreAuthTokenProvider : FirestoreAuthTokenProvider {
    override suspend fun getIdToken(): String? {
        return null
    }
}

