package com.hhp227.concafe.presentation.auth.signin

interface KakaoIdTokenProvider {
    suspend fun getKakaoIdToken(): String
}
