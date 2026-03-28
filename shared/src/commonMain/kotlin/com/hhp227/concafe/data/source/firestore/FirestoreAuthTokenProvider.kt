package com.hhp227.concafe.data.source.firestore

data class FirebaseAuthSession(
    val userId: String,
    val email: String,
    val displayName: String?,
    val idToken: String?,
    val refreshToken: String?,
    val expiresAtEpochSeconds: Long?
)

interface FirestoreAuthTokenProvider {
    suspend fun getIdToken(): String?

    suspend fun signInAnonymously(): FirebaseAuthSession?

    suspend fun signInWithEmailPassword(email: String, password: String): FirebaseAuthSession?

    suspend fun signInWithGoogleIdToken(idToken: String): FirebaseAuthSession?

    suspend fun signInWithAppleIdToken(idToken: String): FirebaseAuthSession?

    suspend fun signInWithKakaoIdToken(idToken: String): FirebaseAuthSession?

    suspend fun signUpWithEmailPassword(email: String, password: String): FirebaseAuthSession?

    suspend fun signOut()

    suspend fun sendPasswordResetEmail(email: String)

    suspend fun updateCurrentUserPassword(
        idToken: String,
        newPassword: String
    ): FirebaseAuthSession?

    suspend fun deleteCurrentUser(idToken: String?)

    suspend fun refreshSession(session: FirebaseAuthSession): FirebaseAuthSession?

    fun getCurrentUserId(): String?

    fun getCurrentUserEmail(): String?

    fun supportsEmailPasswordAuth(): Boolean
}
