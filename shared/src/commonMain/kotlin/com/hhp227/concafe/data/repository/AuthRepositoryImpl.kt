package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl : AuthRepository {
    override suspend fun signIn(email: String, password: String): User {
        TODO("Not yet implemented")
    }

    override suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String?
    ): User {
        TODO("Not yet implemented")
    }

    override suspend fun signOut() {
        TODO("Not yet implemented")
    }

    override suspend fun restoreSession(): User? {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentUser(): User? {
        TODO("Not yet implemented")
    }

    override fun observeCurrentUser(): Flow<User?> {
        TODO("Not yet implemented")
    }
}