package com.hhp227.concafe.data.source.firestore

interface FirebaseAuthRestClient {
    suspend fun postJson(url: String, body: String): String

    suspend fun postFormUrlEncoded(url: String, body: String): String
}
