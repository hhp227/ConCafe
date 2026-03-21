package com.hhp227.concafe.data.source.firestore

data class FirebaseAuthSession(
    val userId: String,
    val email: String,
    val idToken: String?
)

interface FirestoreAuthTokenProvider {
    suspend fun getIdToken(): String?

    suspend fun signInWithEmailPassword(email: String, password: String): FirebaseAuthSession?

    suspend fun signUpWithEmailPassword(email: String, password: String): FirebaseAuthSession?

    suspend fun signOut()

    fun getCurrentUserId(): String?

    fun getCurrentUserEmail(): String?

    fun supportsEmailPasswordAuth(): Boolean
}

class NoOpFirestoreAuthTokenProvider : FirestoreAuthTokenProvider {
    override suspend fun getIdToken(): String? {
        return null
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        return null
    }

    override suspend fun signUpWithEmailPassword(email: String, password: String): FirebaseAuthSession? {
        return null
    }

    override suspend fun signOut() {
    }

    override fun getCurrentUserId(): String? {
        return null
    }

    override fun getCurrentUserEmail(): String? {
        return null
    }

    override fun supportsEmailPasswordAuth(): Boolean {
        return false
    }
}
