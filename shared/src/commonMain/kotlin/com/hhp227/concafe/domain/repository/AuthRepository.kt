package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.AuthProvider
import com.hhp227.concafe.domain.model.DeleteAccountRequest
import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines

interface AuthRepository {
    suspend fun signIn(email: String, password: String): User

    suspend fun signInWithGoogle(): User

    suspend fun signInWithKakao(): User

    suspend fun signInWithGoogleIdToken(idToken: String): User

    suspend fun signInWithAppleIdToken(idToken: String): User

    suspend fun signInWithKakaoIdToken(
        idToken: String,
        email: String? = null,
        nickname: String? = null
    ): User

    suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String? = null
    ): User

    suspend fun completeSignUpForCurrentUser(
        email: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String? = null,
        phoneNumber: String? = null
    ): User

    suspend fun signOut()

    suspend fun requestPhoneVerificationCode(phoneNumber: String)

    suspend fun verifyPhoneVerificationCode(code: String)

    suspend fun linkPhoneCredential(code: String)

    suspend fun linkEmailCredential(email: String, password: String)

    suspend fun discardIncompleteSignUp()

    suspend fun requestPasswordReset(email: String)

    suspend fun changePassword(currentPassword: String, newPassword: String)

    suspend fun deleteAccount(request: DeleteAccountRequest)

    suspend fun getCurrentAuthProvider(): AuthProvider

    suspend fun restoreSession(): User?

    suspend fun getCurrentUser(): User?

    @NativeCoroutines
    fun observeCurrentUser(): Flow<User?>
}
