package com.hhp227.concafe.presentation.auth.signup

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class AndroidPhoneAuthProvider(
    private val activityProvider: () -> Activity?
) : com.hhp227.concafe.presentation.auth.signup.PhoneAuthProvider {
    private val auth = FirebaseAuth.getInstance()

    private var verificationId: String? = null

    override suspend fun sendCode(phoneNumber: String): AppResult<Unit> {
        val activity = activityProvider()
            ?: return AppResult.Failure(AppError.Unknown("activity not available"))
        return suspendCancellableCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (continuation.isActive) continuation.resume(AppResult.Success(Unit))
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (continuation.isActive) {
                        val reason = if (e is FirebaseAuthException) {
                            e.errorCode.ifBlank { e.message ?: "phone verification failed" }
                        } else {
                            e.message ?: "phone verification failed"
                        }
                        continuation.resume(
                            AppResult.Failure(AppError.ValidationFailed(reason))
                        )
                    }
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = newVerificationId
                    if (continuation.isActive) continuation.resume(AppResult.Success(Unit))
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyCode(code: String): AppResult<Unit> {
        val id = verificationId
            ?: return AppResult.Failure(AppError.ValidationFailed("인증번호 요청을 먼저 해주세요."))
        return try {
            val credential = PhoneAuthProvider.getCredential(id, code)

            auth.signInWithCredential(credential).await()
            auth.signOut()
            AppResult.Success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AppResult.Failure(AppError.ValidationFailed("invalid code"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
