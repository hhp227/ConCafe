package com.hhp227.concafe.presentation.auth.signin

data class KakaoAuthPayload(
    val idToken: String,
    val email: String?,
    val nickname: String?
)

interface KakaoIdTokenProvider {
    suspend fun getKakaoAuthPayload(): KakaoAuthPayload
}
