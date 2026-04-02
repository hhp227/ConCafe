package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
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
    private val castRemoteDataSource: CastRemoteDataSource,
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
                val user = resolveUserFromSession(session.userId, session.email, session.displayName)
                authDataSource.currentUserId = user.id
                return user
            }
        }
        throw IllegalArgumentException("invalid credentials")
    }

    override suspend fun signInWithGoogleIdToken(idToken: String): User {
        if (!idToken.isBlank()) {
            val session = authTokenProvider.signInWithGoogleIdToken(idToken)
                ?: throw IllegalArgumentException("google sign-in is not supported")
            val user = resolveUserFromSession(session.userId, session.email, session.displayName)
            authDataSource.currentUserId = user.id
            return user
        }
        throw IllegalArgumentException("google idToken is required")
    }

    override suspend fun signInWithAppleIdToken(idToken: String): User {
        if (!idToken.isBlank()) {
            val session = authTokenProvider.signInWithAppleIdToken(idToken)
                ?: throw IllegalArgumentException("apple sign-in is not supported")
            val user = resolveUserFromSession(session.userId, session.email, session.displayName)
            authDataSource.currentUserId = user.id
            return user
        }
        throw IllegalArgumentException("apple idToken is required")
    }

    override suspend fun signInWithKakaoIdToken(
        idToken: String,
        email: String?,
        nickname: String?
    ): User {
        if (!idToken.isBlank()) {
            val session = authTokenProvider.signInWithKakaoIdToken(idToken)
                ?: throw IllegalArgumentException("kakao sign-in is not supported")
            val resolvedEmail = resolveKakaoEmail(session.email, email)
            val resolvedDisplayName = resolveKakaoDisplayName(session.displayName, nickname)
            val user = resolveUserFromSession(session.userId, resolvedEmail, resolvedDisplayName)
            authDataSource.currentUserId = user.id
            return user
        }
        throw IllegalArgumentException("kakao idToken is required")
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

        if (role == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
            castRemoteDataSource.setAffiliatedCafeId(user.id, affiliatedCafeId)
        }
        val pushResult = runCatching { firestoreSyncDataSource.pushUser(user) }

        if (pushResult.isFailure) {
            if (role == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
                castRemoteDataSource.clearAffiliatedCafeId(user.id)
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

    override suspend fun requestPasswordReset(email: String) {
        if (email.isBlank()) {
            throw IllegalArgumentException("email is required")
        }
        if (!authTokenProvider.supportsEmailPasswordAuth()) {
            throw IllegalArgumentException("email/password auth not supported")
        }
        authTokenProvider.sendPasswordResetEmail(email)
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
            ?: firestoreSyncDataSource.fetchUser(currentUserId)?.email
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
            ?: firestoreSyncDataSource.fetchUser(currentUserId)?.email
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

    private suspend fun resolveUserFromSession(userId: String, email: String, displayName: String?): User {
        val remoteUser = firestoreSyncDataSource.fetchUser(userId)

        if (remoteUser != null) {
            return remoteUser
        }

        val createdUser = User(
            id = userId,
            email = email,
            nickname = resolveInitialNickname(email, displayName),
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = nowIsoUtc()
        )

        runCatching { firestoreSyncDataSource.pushUser(createdUser) }
        return createdUser
    }

    private suspend fun syncCurrentUserIdFromFirebase() {
        if (authTokenProvider.supportsEmailPasswordAuth()) {
            val currentUserIdFromSession = runCatching {
                authTokenProvider.getCurrentUserId()
            }.getOrNull()

            if (!currentUserIdFromSession.isNullOrBlank()) {
                authDataSource.currentUserId = currentUserIdFromSession
            } else {
                Unit
            }
        }
    }

    private suspend fun resolveCurrentUser(): User? {
        val currentUserId = authDataSource.currentUserId ?: return null
        val remoteUser = firestoreSyncDataSource.fetchUser(currentUserId)

        if (remoteUser != null) {
            return remoteUser
        }

        val currentUserEmail = authTokenProvider.getCurrentUserEmail()

        if (currentUserEmail.isNullOrBlank()) {
            return null
        }
        val fallbackUser = User(
            id = currentUserId,
            email = currentUserEmail,
            nickname = resolveInitialNickname(currentUserEmail, null),
            profileImage = null,
            role = UserRole.VISITOR,
            banned = false,
            createdAt = nowIsoUtc()
        )
        runCatching { firestoreSyncDataSource.pushUser(fallbackUser) }
        return fallbackUser
    }
}

private fun resolveInitialNickname(email: String, displayName: String?): String {
    val normalizedDisplayName = displayName?.trim().orEmpty()
    val emailPrefix = email.substringBefore("@").trim()
    return if (normalizedDisplayName.isNotBlank()) {
        normalizedDisplayName
    } else if (emailPrefix.startsWith("anonymous-")) {
        "사용자"
    } else if (emailPrefix.startsWith("kakao-")) {
        "카카오유저"
    } else if (emailPrefix.isNotBlank()) {
        emailPrefix
    } else {
        "유저"
    }
}

private fun resolveKakaoEmail(sessionEmail: String, profileEmail: String?): String {
    val normalizedProfileEmail = profileEmail?.trim().orEmpty()
    return if (normalizedProfileEmail.isNotBlank()) {
        normalizedProfileEmail
    } else {
        sessionEmail
    }
}

private fun resolveKakaoDisplayName(sessionDisplayName: String?, profileNickname: String?): String? {
    val normalizedProfileNickname = profileNickname?.trim().orEmpty()
    return if (normalizedProfileNickname.isNotBlank()) {
        normalizedProfileNickname
    } else {
        sessionDisplayName
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
