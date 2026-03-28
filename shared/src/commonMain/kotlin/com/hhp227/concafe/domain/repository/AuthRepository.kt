package com.hhp227.concafe.domain.repository

import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines

interface AuthRepository {
    suspend fun signIn(email: String, password: String): User

    suspend fun signInWithGoogleIdToken(idToken: String): User

    suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String? = null
    ): User

    suspend fun signOut()

    suspend fun requestPasswordReset(email: String)

    suspend fun changePassword(currentPassword: String, newPassword: String)

    suspend fun deleteAccount(password: String)

    suspend fun restoreSession(): User?

    suspend fun getCurrentUser(): User?

    @NativeCoroutines
    fun observeCurrentUser(): Flow<User?>
}
