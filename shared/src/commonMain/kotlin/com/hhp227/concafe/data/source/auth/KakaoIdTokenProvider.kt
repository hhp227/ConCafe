package com.hhp227.concafe.data.source.auth

data class KakaoAuthPayload(
    val idToken: String,
    val email: String?,
    val nickname: String?
)

/**
 * Obtains a Kakao OIDC ID token plus the profile hints (email, nickname) the Kakao account
 * exposes. Throws when the user cancels or the flow fails.
 */
interface KakaoIdTokenProvider {
    suspend fun getKakaoAuthPayload(): KakaoAuthPayload
}
