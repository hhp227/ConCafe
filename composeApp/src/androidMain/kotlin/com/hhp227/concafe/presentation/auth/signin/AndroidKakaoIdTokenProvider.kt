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
        println("TEST, Kakao getKakaoIdToken started")
        val idToken = if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            runCatching {
                println("TEST, KakaoTalk login available. try talk login")
                loginWithKakaoTalk(activity)
            }.getOrElse {
                println("TEST, KakaoTalk login failed. fallback to account login: ${it.message}")
                loginWithKakaoAccount(activity)
            }
        } else {
            println("TEST, KakaoTalk login not available. use account login")
            loginWithKakaoAccount(activity)
        }
        val nickname = requestKakaoNickname()
        return KakaoAuthPayload(
            idToken = idToken,
            nickname = nickname
        )
    }

    private suspend fun loginWithKakaoTalk(activity: Activity): String {
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
                if (error != null) {
                    println("TEST, loginWithKakaoTalk error: ${error.message}")
                    continuation.resumeWithException(error)
                } else {
                    println("TEST, loginWithKakaoTalk success callback. hasIdToken=${!token?.idToken.isNullOrBlank()}")
                    resolveIdToken(activity, token?.idToken) { idToken, resolveError ->
                        if (resolveError != null) {
                            println("TEST, resolveIdToken after KakaoTalk failed: ${resolveError.message}")
                            continuation.resumeWithException(resolveError)
                        } else if (idToken.isNullOrBlank()) {
                            println("TEST, resolveIdToken after KakaoTalk returned empty idToken")
                            continuation.resumeWithException(
                                IllegalStateException("kakao idToken is missing. configure openid scope in Kakao/Firebase.")
                            )
                        } else {
                            println("TEST, resolveIdToken after KakaoTalk success")
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
                    println("TEST, loginWithKakaoAccount error: ${error.message}")
                    continuation.resumeWithException(error)
                } else {
                    println("TEST, loginWithKakaoAccount success callback. hasIdToken=${!token?.idToken.isNullOrBlank()}")
                    resolveIdToken(activity, token?.idToken) { idToken, resolveError ->
                        if (resolveError != null) {
                            println("TEST, resolveIdToken after KakaoAccount failed: ${resolveError.message}")
                            continuation.resumeWithException(resolveError)
                        } else if (idToken.isNullOrBlank()) {
                            println("TEST, resolveIdToken after KakaoAccount returned empty idToken")
                            continuation.resumeWithException(
                                IllegalStateException("kakao idToken is missing. configure openid scope in Kakao/Firebase.")
                            )
                        } else {
                            println("TEST, resolveIdToken after KakaoAccount success")
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
            println("TEST, current Kakao idToken exists")
            completion(currentIdToken, null)
            return
        }
        println("TEST, current Kakao idToken missing. request new scope openid only")
        UserApiClient.instance.loginWithNewScopes(activity, listOf("openid")) { token, error ->
            if (error != null) {
                println("TEST, loginWithNewScopes error: ${error.message}")
                completion(null, error)
            } else {
                val refreshedIdToken = token?.idToken
                println("TEST, loginWithNewScopes success. hasIdToken=${!refreshedIdToken.isNullOrBlank()}")
                completion(refreshedIdToken, null)
            }
        }
    }

    private suspend fun requestKakaoNickname(): String? {
        return suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    println("TEST, requestKakaoNickname error: ${error.message}")
                    continuation.resume(null)
                } else {
                    val nickname = user?.kakaoAccount?.profile?.nickname?.trim()
                    println("TEST, requestKakaoNickname success. hasNickname=${!nickname.isNullOrBlank()}")
                    continuation.resume(nickname?.ifBlank { null })
                }
            }
        }
    }
}
