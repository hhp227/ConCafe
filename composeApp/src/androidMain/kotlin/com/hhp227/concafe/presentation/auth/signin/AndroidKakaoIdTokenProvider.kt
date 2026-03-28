package com.hhp227.concafe.presentation.auth.signin

import android.app.Activity
import com.kakao.sdk.user.UserApiClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidKakaoIdTokenProvider(
    private val activityProvider: () -> Activity?
) : KakaoIdTokenProvider {
    override suspend fun getKakaoAuthPayload(): KakaoAuthPayload {
        val activity = activityProvider()
            ?: throw IllegalStateException("kakao sign-in requires current activity")
        val idToken = if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            runCatching {
                loginWithKakaoTalk(activity)
            }.getOrElse {
                loginWithKakaoAccount(activity)
            }
        } else {
            loginWithKakaoAccount(activity)
        }
        val profile = requestKakaoProfile()
        return KakaoAuthPayload(
            idToken = idToken,
            email = profile.email,
            nickname = profile.nickname
        )
    }

    private suspend fun loginWithKakaoTalk(activity: Activity): String {
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                if (error != null) {
                    continuation.resumeWithException(error)
                } else {
                    resolveIdToken(activity, token?.idToken) { idToken, resolveError ->
                        if (resolveError != null) {
                            continuation.resumeWithException(resolveError)
                        } else if (idToken.isNullOrBlank()) {
                            continuation.resumeWithException(
                                IllegalStateException("kakao idToken is missing. configure openid scope in Kakao/Firebase.")
                            )
                        } else {
                            continuation.resume(idToken)
                        }
                    }
                }
            }
        }
    }

    private suspend fun loginWithKakaoAccount(activity: Activity): String {
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
                if (error != null) {
                    continuation.resumeWithException(error)
                } else {
                    resolveIdToken(activity, token?.idToken) { idToken, resolveError ->
                        if (resolveError != null) {
                            continuation.resumeWithException(resolveError)
                        } else if (idToken.isNullOrBlank()) {
                            continuation.resumeWithException(
                                IllegalStateException("kakao idToken is missing. configure openid scope in Kakao/Firebase.")
                            )
                        } else {
                            continuation.resume(idToken)
                        }
                    }
                }
            }
        }
    }

    private fun resolveIdToken(
        activity: Activity,
        currentIdToken: String?,
        completion: (String?, Throwable?) -> Unit
    ) {
        if (!currentIdToken.isNullOrBlank()) {
            completion(currentIdToken, null)
            return
        }
        UserApiClient.instance.loginWithNewScopes(activity, listOf("openid")) { token, error ->
            if (error != null) {
                completion(null, error)
            } else {
                val refreshedIdToken = token?.idToken
                completion(refreshedIdToken, null)
            }
        }
    }

    private suspend fun requestKakaoProfile(): KakaoProfile {
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    continuation.resume(KakaoProfile(email = null, nickname = null))
                } else {
                    val nickname = user?.kakaoAccount?.profile?.nickname?.trim()
                    val email = user?.kakaoAccount?.email?.trim()
                    continuation.resume(
                        KakaoProfile(
                            email = email?.ifBlank { null },
                            nickname = nickname?.ifBlank { null }
                        )
                    )
                }
            }
        }
    }
}

private data class KakaoProfile(
    val email: String?,
    val nickname: String?
)
