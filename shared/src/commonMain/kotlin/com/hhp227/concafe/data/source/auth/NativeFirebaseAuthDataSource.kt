package com.hhp227.concafe.data.source.auth

/**
 * Platform Firebase Auth SDK session, kept in step with the REST session managed by
 * FirestoreAuthTokenProvider. SDK-only features (phone verification, credential linking)
 * operate on this session.
 *
 * Social sign-ins return the SDK session's uid, or null when the platform has no SDK session
 * (e.g. Desktop); the caller verifies the uid matches the REST session. Phone and link
 * operations throw with the Firebase error code as the message; platforms without an SDK
 * session throw UnsupportedOperationException.
 */
interface NativeFirebaseAuthDataSource {
    suspend fun signInWithGoogleIdToken(idToken: String): String?

    suspend fun signInWithAppleIdToken(idToken: String): String?

    suspend fun signInWithKakaoIdToken(idToken: String): String?

    suspend fun signOut()

    suspend fun sendPhoneVerificationCode(phoneNumber: String)

    suspend fun signInWithPhoneVerificationCode(code: String)

    suspend fun linkPhoneCredential(code: String)

    suspend fun linkEmailCredential(email: String, password: String)

    suspend fun deleteCurrentUser()
}
