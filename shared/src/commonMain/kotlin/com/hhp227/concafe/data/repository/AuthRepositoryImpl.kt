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
        return authDataSource.findUserByEmail(email)?.also {
            authDataSource.currentUserId = it.id
        } ?: throw IllegalArgumentException("invalid credentials")
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): User {
        if (!idToken.isBlank()) {
            val session = authTokenProvider.signInWithGoogleIdToken(idToken)
                ?: throw IllegalArgumentException("google sign-in is not supported")
            val user = resolveUserFromSession(session.userId, session.email)
            authDataSource.currentUserId = user.id
            return user
        }
        throw IllegalArgumentException("google idToken is required")
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

        val signUpSession = if (authTokenProvider.supportsEmailPasswordAuth()) {
            authTokenProvider.signUpWithEmailPassword(email, password)
        } else {
            null
        }
        val user = User(
            id = signUpSession?.userId ?: nextEntityId("user"),
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
        val pushResult = runCatching { firestoreSyncDataSource.pushUser(user) }

        if (pushResult.isFailure) {
            authDataSource.removeUser(user.id)

            if (role == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
                castDataSource.affiliatedCafeIdByUser.remove(user.id)
            }
            if (signUpSession != null) {
                runCatching {
                    authTokenProvider.deleteCurrentUser(
                        signUpSession.idToken ?: authTokenProvider.getIdToken()
                    )
                }
                runCatching { authTokenProvider.signOut() }
            }
            throw IllegalStateException(
                pushResult.exceptionOrNull()?.message ?: "failed to persist signup profile"
            )
        }

        authDataSource.currentUserId = user.id
        return user
    }

    override suspend fun signOut() {
        if (authTokenProvider.supportsEmailPasswordAuth()) {
            authTokenProvider.signOut()
        }
        authDataSource.currentUserId = null
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            throw IllegalArgumentException("currentPassword/newPassword is required")
        }
        if (!authTokenProvider.supportsEmailPasswordAuth()) {
            throw IllegalArgumentException("email/password auth not supported")
        }

        val currentUserId = authTokenProvider.getCurrentUserId()
            ?: authDataSource.currentUserId
            ?: throw IllegalArgumentException("no signed in user")
        val currentUserEmail = authTokenProvider.getCurrentUserEmail()
            ?: authDataSource.findUserById(currentUserId)?.email
            ?: throw IllegalArgumentException("current user email not found")
        val verifiedSession = try {
            authTokenProvider.signInWithEmailPassword(
                email = currentUserEmail,
                password = currentPassword
            ) ?: throw IllegalArgumentException("invalid password")
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            if (isInvalidPasswordError(e)) {
                throw IllegalArgumentException("invalid password")
            } else {
                throw e
            }
        }

        if (verifiedSession.userId != currentUserId) {
            throw IllegalArgumentException("password does not match current user")
        }

        authTokenProvider.updateCurrentUserPassword(
            idToken = verifiedSession.idToken ?: authTokenProvider.getIdToken()
            ?: throw IllegalStateException("Firebase auth update requires idToken"),
            newPassword = newPassword
        ) ?: throw IllegalStateException("failed to update password in firebase auth")
    }

    override suspend fun deleteAccount(password: String) {
        if (password.isBlank()) {
            throw IllegalArgumentException("password is required")
        }
        if (!authTokenProvider.supportsEmailPasswordAuth()) {
            throw IllegalArgumentException("email/password auth not supported")
        }

        // Prefer provider session as source of truth, fallback to cache only when needed.
        val currentUserId = authTokenProvider.getCurrentUserId()
            ?: authDataSource.currentUserId
            ?: throw IllegalArgumentException("no signed in user")
        val currentUserEmail = authTokenProvider.getCurrentUserEmail()
            ?: authDataSource.findUserById(currentUserId)?.email
            ?: throw IllegalArgumentException("current user email not found")
        val verifiedSession = try {
            authTokenProvider.signInWithEmailPassword(
                email = currentUserEmail,
                password = password
            ) ?: throw IllegalArgumentException("invalid password")
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            if (isInvalidPasswordError(e)) {
                throw IllegalArgumentException("invalid password")
            } else {
                throw e
            }
        }

        if (verifiedSession.userId != currentUserId) {
            throw IllegalArgumentException("password does not match current user")
        }

        firestoreSyncDataSource.deleteUser(currentUserId)
        authTokenProvider.deleteCurrentUser(verifiedSession.idToken ?: authTokenProvider.getIdToken())
        authDataSource.removeUser(currentUserId)
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
            if (userId == null) null else resolveCurrentUser()
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
        if (authTokenProvider.supportsEmailPasswordAuth()) {
            val idToken = authTokenProvider.getIdToken()
            val firebaseUserId = if (idToken.isNullOrBlank()) {
                null
            } else {
                authTokenProvider.getCurrentUserId()
            }
            authDataSource.currentUserId = firebaseUserId
        }
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
        return null
    }
}

private fun isInvalidPasswordError(error: Exception): Boolean {
    val message = error.message.orEmpty().uppercase()
    return message.contains("INVALID_LOGIN_CREDENTIALS")
        || message.contains("INVALID_PASSWORD")
        || message.contains("EMAIL_NOT_FOUND")
}

private fun nextEntityId(prefix: String): String {
    val now = Clock.System.now().toEpochMilliseconds()
    return "$prefix-$now"
}

private fun nowIsoUtc(): String {
    return Clock.System.now().toString()
}
