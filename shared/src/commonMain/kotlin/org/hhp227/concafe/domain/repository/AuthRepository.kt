package org.hhp227.concafe.domain.repository

import kotlinx.coroutines.flow.Flow
import org.hhp227.concafe.domain.model.User
import org.hhp227.concafe.domain.model.UserRole

interface AuthRepository {
    suspend fun signIn(email: String, password: String): User

    suspend fun signUp(email: String, password: String, nickname: String, role: UserRole): User

    suspend fun signOut()

    suspend fun restoreSession(): User?

    suspend fun getCurrentUser(): User?

    fun observeCurrentUser(): Flow<User?>
}
