package com.hhp227.concafe.presentation.auth.signup

import com.hhp227.concafe.domain.common.AppResult

interface SocialFirebaseAuthProvider {
    suspend fun signInWithGoogleIdToken(
        idToken: String,
        expectedUserId: String
    ): AppResult<Unit>

    suspend fun signInWithKakaoIdToken(
        idToken: String,
        expectedUserId: String
    ): AppResult<Unit>

    suspend fun signOut(): AppResult<Unit>
}
