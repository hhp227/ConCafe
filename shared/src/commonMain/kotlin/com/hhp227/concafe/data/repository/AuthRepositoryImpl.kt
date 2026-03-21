package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.CastDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val castDataSource: CastDataSource,
    private val authTokenProvider: FirestoreAuthTokenProvider,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("email/password is required")
        }

        if (authTokenProvider.supportsEmailPasswordAuth()) {
            val session = authTokenProvider.signInWithEmailPassword(email, password)
            if (session != null) {
                val user = resolveUserFromSession(session.userId, session.email)
                authDataSource.currentUserId = user.id
                return user
            }
        }

        val foundUser = authDataSource.findUserByEmail(email)

        return if (foundUser != null) {
            authDataSource.currentUserId = foundUser.id
            foundUser
        } else {
            throw IllegalArgumentException("invalid credentials")
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

        if (authDataSource.isEmailTaken(email)) {
            throw IllegalArgumentException("email already exists")
        }

        val firebaseUserId = if (authTokenProvider.supportsEmailPasswordAuth()) {
            authTokenProvider.signUpWithEmailPassword(email, password)?.userId
        } else {
            null
        }

        val user = User(
            id = firebaseUserId ?: nextEntityId("user"),
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

        runCatching { firestoreSyncDataSource.pushUser(user) }

        authDataSource.currentUserId = user.id

        return user
    }

    override suspend fun signOut() {
        if (authTokenProvider.supportsEmailPasswordAuth()) {
            authTokenProvider.signOut()
        }

        authDataSource.currentUserId = null
    }

    override suspend fun restoreSession(): User? {
        syncCurrentUserIdFromFirebase()
        return resolveCurrentUser()
    }

    override suspend fun getCurrentUser(): User? {
        syncCurrentUserIdFromFirebase()
        return resolveCurrentUser()
    }

    override fun observeCurrentUser(): Flow<User?> {
        return authDataSource.currentUserIdFlow.map { userId ->
            if (userId == null) {
                null
            } else {
                resolveCurrentUser()
            }
        }
    }

    private suspend fun resolveUserFromSession(userId: String, email: String): User {
        val foundById = authDataSource.findUserById(userId)
        if (foundById != null) {
            return foundById
        }

        val remoteUser = firestoreSyncDataSource.fetchUser(userId)
        if (remoteUser != null) {
            val replaced = authDataSource.replaceUser(remoteUser)
            if (!replaced) {
                authDataSource.addUser(remoteUser)
            }
            return remoteUser
        }

        val foundByEmail = authDataSource.findUserByEmail(email)
        if (foundByEmail != null) {
            val migratedUser = foundByEmail.copy(id = userId, email = email)
            val replaced = authDataSource.replaceUser(migratedUser)

            return if (replaced) {
                migratedUser
            } else {
                foundByEmail
            }
        }

        val createdUser = User(
            id = userId,
            email = email,
            nickname = email.substringBefore("@").ifBlank { "유저" },
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = nowIsoUtc()
        )

        authDataSource.addUser(createdUser)

        runCatching { firestoreSyncDataSource.pushUser(createdUser) }

        return createdUser
    }

    private suspend fun syncCurrentUserIdFromFirebase() {
        if (!authTokenProvider.supportsEmailPasswordAuth()) {
            return
        }

        val firebaseUserId = authTokenProvider.getCurrentUserId()

        authDataSource.currentUserId = firebaseUserId
    }

    private suspend fun resolveCurrentUser(): User? {
        val currentUserId = authDataSource.currentUserId ?: return null
        val localUser = authDataSource.findUserById(currentUserId)

        if (localUser != null) {
            return localUser
        }

        val remoteUser = firestoreSyncDataSource.fetchUser(currentUserId)

        if (remoteUser != null) {
            val replaced = authDataSource.replaceUser(remoteUser)

            if (!replaced) {
                authDataSource.addUser(remoteUser)
            }
            return remoteUser
        }

        val currentUserEmail = authTokenProvider.getCurrentUserEmail()

        if (currentUserEmail.isNullOrBlank()) {
            return null
        }

        val restoredUser = User(
            id = currentUserId,
            email = currentUserEmail,
            nickname = currentUserEmail.substringBefore("@").ifBlank { "유저" },
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = nowIsoUtc()
        )
        val replaced = authDataSource.replaceUser(restoredUser)

        if (!replaced) {
            authDataSource.addUser(restoredUser)
        }
        return restoredUser
    }
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
