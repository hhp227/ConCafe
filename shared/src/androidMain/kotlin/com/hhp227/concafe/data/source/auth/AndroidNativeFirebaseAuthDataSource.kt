package com.hhp227.concafe.data.source.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class AndroidNativeFirebaseAuthDataSource(
    private val activityProvider: () -> Activity?
) : NativeFirebaseAuthDataSource {
    private val auth = FirebaseAuth.getInstance()

    private var verificationId: String? = null

    override suspend fun signInWithGoogleIdToken(idToken: String): String? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    override suspend fun signInWithAppleIdToken(idToken: String): String? {
        val credential = OAuthProvider.newCredentialBuilder(APPLE_PROVIDER_ID)
            .setIdToken(idToken)
            .build()
        return signInWithCredential(credential)
    }

    override suspend fun signInWithKakaoIdToken(idToken: String): String? {
        val credential = OAuthProvider.newCredentialBuilder(KAKAO_OIDC_PROVIDER_ID)
            .setIdToken(idToken)
            .build()
        return signInWithCredential(credential)
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun sendPhoneVerificationCode(phoneNumber: String) {
        val activity = activityProvider()
            ?: throw IllegalStateException("phone verification requires current activity")
        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalArgumentException(e.reason(), e))
                    }
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = newVerificationId
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(PHONE_VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun signInWithPhoneVerificationCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(requireVerificationId(), code)
        runFirebaseAuth { auth.signInWithCredential(credential).await() }
    }

    override suspend fun linkPhoneCredential(code: String) {
        val credential = PhoneAuthProvider.getCredential(requireVerificationId(), code)
        runFirebaseAuth { requireCurrentUser().linkWithCredential(credential).await() }
    }

    override suspend fun linkEmailCredential(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)
        runFirebaseAuth { requireCurrentUser().linkWithCredential(credential).await() }
    }

    override suspend fun deleteCurrentUser() {
        verificationId = null
        val currentUser = auth.currentUser ?: return

        try {
            currentUser.delete().await()
        } catch (e: Exception) {
            runCatching { auth.signOut() }
            throw e
        }
    }

    private suspend fun signInWithCredential(credential: AuthCredential): String {
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("firebase sdk sign-in returned no user")
    }

    private fun requireVerificationId(): String {
        return verificationId ?: throw IllegalStateException("VERIFICATION_NOT_REQUESTED")
    }

    private fun requireCurrentUser() = auth.currentUser
        ?: throw IllegalArgumentException("NO_CURRENT_USER")

    // Surfaces the Firebase error code as the exception message so callers can map it.
    private suspend fun <T> runFirebaseAuth(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: FirebaseAuthException) {
            throw IllegalArgumentException(e.reason(), e)
        }
    }
}

private fun FirebaseException.reason(): String {
    return if (this is FirebaseAuthException) {
        errorCode.ifBlank { message ?: "firebase auth failed" }
    } else {
        message ?: "firebase auth failed"
    }
}

private const val APPLE_PROVIDER_ID = "apple.com"
private const val KAKAO_OIDC_PROVIDER_ID = "oidc.kakao"
private const val PHONE_VERIFICATION_TIMEOUT_SECONDS = 60L
