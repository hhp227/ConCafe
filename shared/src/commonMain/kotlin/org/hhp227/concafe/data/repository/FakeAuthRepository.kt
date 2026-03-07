package org.hhp227.concafe.data.repository

import org.hhp227.concafe.data.source.ConCafeDataSource
import org.hhp227.concafe.domain.model.User
import org.hhp227.concafe.domain.model.UserRole
import org.hhp227.concafe.domain.repository.AuthRepository

class FakeAuthRepository(
    private val dataSource: ConCafeDataSource
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("email/password is required")
        }

        val found = dataSource.users.firstOrNull { it.email == email }

        return if (found != null) {
            dataSource.currentUserId = found.id
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
            created
        }
    }

    override suspend fun signUp(email: String, password: String, nickname: String): User {
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
            role = UserRole.VISITOR,
            banned = false,
            createdAt = "2026-03-05T00:00:00Z"
        )
        dataSource.users.add(user)
        dataSource.currentUserId = user.id
        return user
    }

    override suspend fun signOut() {
        dataSource.currentUserId = null
    }

    override suspend fun restoreSession(): User? {
        return dataSource.users.firstOrNull { it.id == dataSource.currentUserId }
    }

    override suspend fun getCurrentUser(): User? {
        return dataSource.users.firstOrNull { it.id == dataSource.currentUserId }
    }
}
