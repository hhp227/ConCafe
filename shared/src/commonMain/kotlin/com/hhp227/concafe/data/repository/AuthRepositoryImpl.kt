package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreConCafeDataSource
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val castDataSource: CastDataSource
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("email/password is required")
        }

        val found = authDataSource.findUserByEmail(email)

        if (found == null) {
            throw IllegalArgumentException("invalid credentials")
        }

        authDataSource.currentUserId = found.id
        return found
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

        val duplicate = authDataSource.isEmailTaken(email)

        if (duplicate) {
            throw IllegalArgumentException("email already exists")
        }

        val user = User(
            id = nextEntityId("user"),
            email = email,
            nickname = nickname,
            profileImage = null,
            role = role,
            banned = false,
            createdAt = nowIsoUtc()
        )
        authDataSource.addUser(user)

        if (role == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
            castDataSource.affiliatedCafeIdByUser[user.id] = affiliatedCafeId
        }
        val firestoreDataSource = authDataSource as? FirestoreConCafeDataSource
        if (firestoreDataSource != null) {
            runCatching { firestoreDataSource.pushUser(user) }
        }

        authDataSource.currentUserId = user.id
        return user
    }

    override suspend fun signOut() {
        authDataSource.currentUserId = null
    }

    override suspend fun restoreSession(): User? {
        return authDataSource.currentUserId?.let { authDataSource.findUserById(it) }
    }

    override suspend fun getCurrentUser(): User? {
        return authDataSource.currentUserId?.let { authDataSource.findUserById(it) }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return authDataSource.currentUserIdFlow.map { userId ->
            userId?.let { authDataSource.findUserById(it) }
        }
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
