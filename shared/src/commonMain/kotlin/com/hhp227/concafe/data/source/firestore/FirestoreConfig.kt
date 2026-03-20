package com.hhp227.concafe.data.source.firestore

data class FirestoreConfig(
    val projectId: String,
    val databaseId: String = "(default)",
    val baseUrl: String = "https://firestore.googleapis.com/v1"
) {
    fun documentBasePath(): String {
        return "$baseUrl/projects/$projectId/databases/$databaseId/documents"
    }
}

