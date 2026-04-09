package com.hhp227.concafe.presentation.auth.signup

import com.hhp227.concafe.domain.common.AppResult

class JvmSocialFirebaseAuthProvider : SocialFirebaseAuthProvider {
    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        expectedUserId: String
    ): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun signInWithKakaoIdToken(
        idToken: String,
        expectedUserId: String
    ): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun signOut(): AppResult<Unit> = AppResult.Success(Unit)
}
