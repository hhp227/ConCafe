package com.hhp227.concafe.data.source.auth

class JvmNativeFirebaseAuthDataSource : NativeFirebaseAuthDataSource {
    override suspend fun signInWithGoogleIdToken(idToken: String): String? = null

    override suspend fun signInWithAppleIdToken(idToken: String): String? = null

    override suspend fun signInWithKakaoIdToken(idToken: String): String? = null

    override suspend fun signOut() = Unit

    override suspend fun sendPhoneVerificationCode(phoneNumber: String) {
        unsupported()
    }

    override suspend fun signInWithPhoneVerificationCode(code: String) {
        unsupported()
    }

    override suspend fun linkPhoneCredential(code: String) {
        unsupported()
    }

    override suspend fun linkEmailCredential(email: String, password: String) {
        unsupported()
    }

    override suspend fun deleteCurrentUser() = Unit

    private fun unsupported(): Nothing {
        throw UnsupportedOperationException("phone verification is not supported on desktop")
    }
}
