package com.hhp227.concafe.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository

class FakeAuthRepository(
    private val dataSource: ConCafeDataSource
) : AuthRepository {
    private val currentUserFlow = MutableStateFlow<User?>(resolveCurrentUser())

    override suspend fun signIn(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("email/password is required")
        }

        val found = dataSource.users.firstOrNull { it.email == email }

        return if (found != null) {
            dataSource.currentUserId = found.id
            currentUserFlow.value = found
            found
        } else {
            val created = User(
                id = "user-${dataSource.users.size + 1}",
                email = email,
                nickname = "신규유저",
                profileImage = null,
                role = UserRole.VISITOR,
                banned = false,
                createdAt = "2026-03-05T00:00:00Z"
            )
            dataSource.users.add(created)
            dataSource.currentUserId = created.id
            currentUserFlow.value = created
            created
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String?
    ): User {
        if (email.isBlank() || password.isBlank() || nickname.isBlank()) {
            throw IllegalArgumentException("email/password/nickname is required")
        }

        val duplicate = dataSource.users.any { it.email == email }

        if (duplicate) {
            throw IllegalArgumentException("email already exists")
        }

        val user = User(
            id = "user-${dataSource.users.size + 1}",
            email = email,
            nickname = nickname,
            profileImage = null,
            role = role,
            banned = false,
            createdAt = "2026-03-05T00:00:00Z"
        )
        dataSource.users.add(user)
        if (role == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
            dataSource.affiliatedCafeIdByUser[user.id] = affiliatedCafeId
        }
        dataSource.currentUserId = user.id
        currentUserFlow.value = user
        return user
    }

    override suspend fun signOut() {
        dataSource.currentUserId = null
        currentUserFlow.value = null
    }

    override suspend fun restoreSession(): User? {
        val restored = dataSource.users.firstOrNull { it.id == dataSource.currentUserId }
        currentUserFlow.value = restored
        return restored
    }

    override suspend fun getCurrentUser(): User? {
        return dataSource.users.firstOrNull { it.id == dataSource.currentUserId }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return currentUserFlow.asStateFlow()
    }

    private fun resolveCurrentUser(): User? {
        return dataSource.users.firstOrNull { it.id == dataSource.currentUserId }
    }
}
