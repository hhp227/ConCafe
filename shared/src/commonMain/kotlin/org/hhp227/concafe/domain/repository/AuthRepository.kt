package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): User

    suspend fun signUp(email: String, password: String, nickname: String): User

    suspend fun signOut()

    suspend fun restoreSession(): User?

    suspend fun getCurrentUser(): User?
}
