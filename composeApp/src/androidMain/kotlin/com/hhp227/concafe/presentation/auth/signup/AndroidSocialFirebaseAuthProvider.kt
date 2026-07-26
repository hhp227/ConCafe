package com.hhp227.concafe.presentation.auth.signup

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import kotlinx.coroutines.tasks.await

class AndroidSocialFirebaseAuthProvider : SocialFirebaseAuthProvider {
    private val auth = FirebaseAuth.getInstance()

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        expectedUserId: String
    ): AppResult<Unit> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential, expectedUserId)
    }

    override suspend fun signInWithKakaoIdToken(
        idToken: String,
        expectedUserId: String
    ): AppResult<Unit> {
        val credential = OAuthProvider.newCredentialBuilder(KAKAO_OIDC_PROVIDER_ID)
            .setIdToken(idToken)
            .build()
        return signInWithCredential(credential, expectedUserId)
    }

    private suspend fun signInWithCredential(
        credential: AuthCredential,
        expectedUserId: String
    ): AppResult<Unit> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val actualUserId = result.user?.uid

            if (actualUserId.isNullOrBlank() || actualUserId != expectedUserId) {
                runCatching { auth.signOut() }
                AppResult.Failure(AppError.ValidationFailed("SOCIAL_SESSION_USER_MISMATCH"))
            } else {
                AppResult.Success(Unit)
            }
        } catch (e: FirebaseAuthException) {
            val reason = e.errorCode.ifBlank { e.message ?: "social firebase sign-in failed" }
            AppResult.Failure(AppError.ValidationFailed(reason))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    override suspend fun signOut(): AppResult<Unit> {
        return try {
            auth.signOut()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}

private const val KAKAO_OIDC_PROVIDER_ID = "oidc.kakao"
