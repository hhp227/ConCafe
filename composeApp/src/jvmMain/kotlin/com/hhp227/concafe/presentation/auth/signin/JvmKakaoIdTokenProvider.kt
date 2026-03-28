package com.hhp227.concafe.presentation.auth.signin

class JvmKakaoIdTokenProvider : KakaoIdTokenProvider {
    override suspend fun getKakaoIdToken(): String {
        throw IllegalStateException("카카오 로그인은 Android/iOS 앱에서 지원됩니다.")
    }
}
