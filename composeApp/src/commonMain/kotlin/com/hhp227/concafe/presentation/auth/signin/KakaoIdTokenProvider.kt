package com.hhp227.concafe.presentation.auth.signin

data class KakaoAuthPayload(
    val idToken: String,
    val nickname: String?
)

interface KakaoIdTokenProvider {
    suspend fun getKakaoAuthPayload(): KakaoAuthPayload
}
