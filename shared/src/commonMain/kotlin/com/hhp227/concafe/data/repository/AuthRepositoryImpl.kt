package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.CastRemoteDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreAuthTokenProvider
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.AuthProvider
import com.hhp227.concafe.domain.model.DeleteAccountRequest
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.policy.DormantAccountPolicy
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
    private val dormantAccountPolicy = DormantAccountPolicy()

    private suspend fun resolveEffectiveRole(
        userId: String,
        baseRole: UserRole
    ): UserRole {
        val linkedCast = runCatching {
            castRemoteDataSource.fetchCastByLinkedUserId(userId)
        }.getOrNull()
        val resolvedRole = if (baseRole == UserRole.VISITOR && linkedCast != null) UserRole.CAST else baseRole
        return resolvedRole
    }

    private suspend fun normalizeRoleIfNeeded(user: User): User {
        val resolvedRole = resolveEffectiveRole(
            userId = user.id,
            baseRole = user.role
        )
        val normalizedUser = if (resolvedRole != user.role) {
            user.copy(role = resolvedRole)
        } else {
            user
        }
        if (normalizedUser.role != user.role) {
            runCatching {
                firestoreSyncDataSource.pushUser(normalizedUser)
            }
            println(
                "TEST, AuthRepositoryImpl normalizeRoleIfNeeded role-updated: " +
                    "userId=${user.id} from=${user.role} to=${normalizedUser.role}"
            )
        }
        return normalizedUser
    }

    private suspend fun applyLoginActivity(user: User): User {
        val now = Clock.System.now()
        val shouldReleaseDormant = user.dormant
        val shouldRefreshLastLogin = dormantAccountPolicy.shouldRefreshLastLogin(user.lastLoginAt, now)

        return if (shouldReleaseDormant || shouldRefreshLastLogin) {
            val lastLoginAt = now.toString()

            runCatching { firestoreSyncDataSource.updateUserLastLogin(user.id, lastLoginAt) }
            user.copy(lastLoginAt = lastLoginAt, dormant = false, dormantAt = null)
        } else {
            user
        }
    }

    override suspend fun signIn(email: String, password: String): User {
        if (email.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("email/password is required")
        }

        if (authTokenProvider.supportsEmailPasswordAuth()) {
            val session = authTokenProvider.signInWithEmailPassword(email, password)

            if (session != null) {
                val user = resolveUserFromSession(
                    userId = session.userId,
                    email = session.email,
                    displayName = session.displayName,
                    authProvider = AuthProvider.EMAIL
                )
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
            val user = resolveUserFromSession(
                userId = session.userId,
                email = session.email,
                displayName = session.displayName,
                authProvider = AuthProvider.GOOGLE
            )
            authDataSource.currentUserId = user.id
            return user
        }
        throw IllegalArgumentException("google idToken is required")
    }

    override suspend fun signInWithAppleIdToken(idToken: String): User {
        if (!idToken.isBlank()) {
            val session = authTokenProvider.signInWithAppleIdToken(idToken)
                ?: throw IllegalArgumentException("apple sign-in is not supported")
            val user = resolveUserFromSession(
                userId = session.userId,
                email = session.email,
                displayName = session.displayName,
                authProvider = AuthProvider.APPLE
            )
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
            val user = resolveUserFromSession(
                userId = session.userId,
                email = resolvedEmail,
                displayName = resolvedDisplayName,
                authProvider = AuthProvider.KAKAO
            )
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
            authProvider = AuthProvider.EMAIL,
            role = role,
            banned = false,
            createdAt = nowIsoUtc(),
            signupCompleted = true
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
        authTokenProvider.setCachedSignupCompleted(true)
        return user
    }

    override suspend fun completeSignUpForCurrentUser(
        email: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String?,
        phoneNumber: String?
    ): User {
        val normalizedEmail = email.trim()
        val normalizedNickname = nickname.trim()
        val normalizedPhoneNumber = phoneNumber?.trim()?.ifBlank { null }

        if (normalizedEmail.isBlank() || normalizedNickname.isBlank()) {
            throw IllegalArgumentException("email/nickname is required")
        }
        val currentUserId = authTokenProvider.getCurrentUserId()
            ?: authDataSource.currentUserId
            ?: throw IllegalArgumentException("no signed in user")
        val existingUser = firestoreSyncDataSource.fetchUser(currentUserId)
        val resolvedRole = resolveEffectiveRole(
            userId = currentUserId,
            baseRole = role
        )
        val user = User(
            id = currentUserId,
            email = normalizedEmail,
            nickname = normalizedNickname,
            profileImage = existingUser?.profileImage,
            authProvider = existingUser?.authProvider?.takeIf { it != AuthProvider.UNKNOWN }
                ?: authTokenProvider.getCurrentAuthProvider(),
            role = resolvedRole,
            banned = existingUser?.banned ?: false,
            createdAt = existingUser?.createdAt ?: nowIsoUtc(),
            phoneNumber = normalizedPhoneNumber ?: existingUser?.phoneNumber,
            signupCompleted = true
        )

        if (resolvedRole == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
            castRemoteDataSource.setAffiliatedCafeId(currentUserId, affiliatedCafeId)
        }
        firestoreSyncDataSource.pushUser(user)
        authDataSource.currentUserId = user.id
        authTokenProvider.setCachedSignupCompleted(true)
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

    override suspend fun deleteAccount(request: DeleteAccountRequest) {
        val currentUserId = authTokenProvider.getCurrentUserId()
            ?: authDataSource.currentUserId
            ?: throw IllegalArgumentException("no signed in user")
        val resolvedProvider = if (request.provider == AuthProvider.UNKNOWN) {
            getCurrentAuthProvider()
        } else {
            request.provider
        }
        val verifiedIdToken = when (resolvedProvider) {
            AuthProvider.EMAIL, AuthProvider.UNKNOWN -> {
                val password = request.password?.trim().orEmpty()
                if (password.isBlank()) {
                    throw IllegalArgumentException("password is required")
                }
                if (!authTokenProvider.supportsEmailPasswordAuth()) {
                    throw IllegalArgumentException("email/password auth not supported")
                }

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
                verifiedSession.idToken ?: authTokenProvider.getIdToken()
            }
            AuthProvider.GOOGLE -> {
                val socialIdToken = request.idToken?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("social idToken is required")
                val verifiedSession = authTokenProvider.signInWithGoogleIdToken(socialIdToken)
                    ?: throw IllegalArgumentException("google re-auth failed")
                if (verifiedSession.userId != currentUserId) {
                    throw IllegalArgumentException("social credential does not match current user")
                }
                verifiedSession.idToken ?: authTokenProvider.getIdToken()
            }
            AuthProvider.APPLE -> {
                val socialIdToken = request.idToken?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("social idToken is required")
                val verifiedSession = authTokenProvider.signInWithAppleIdToken(socialIdToken)
                    ?: throw IllegalArgumentException("apple re-auth failed")
                if (verifiedSession.userId != currentUserId) {
                    throw IllegalArgumentException("social credential does not match current user")
                }
                verifiedSession.idToken ?: authTokenProvider.getIdToken()
            }
            AuthProvider.KAKAO -> {
                val socialIdToken = request.idToken?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("social idToken is required")
                val verifiedSession = authTokenProvider.signInWithKakaoIdToken(socialIdToken)
                    ?: throw IllegalArgumentException("kakao re-auth failed")
                if (verifiedSession.userId != currentUserId) {
                    throw IllegalArgumentException("social credential does not match current user")
                }
                verifiedSession.idToken ?: authTokenProvider.getIdToken()
            }
        }

        val resolvedVerifiedIdToken = verifiedIdToken
            ?: throw IllegalStateException("delete account requires verified Firebase idToken")
        firestoreSyncDataSource.deleteCurrentUserCascade(resolvedVerifiedIdToken)
        authTokenProvider.signOut()
        authDataSource.currentUserId = null
    }

    override suspend fun getCurrentAuthProvider(): AuthProvider {
        val currentUserId = authTokenProvider.getCurrentUserId()
            ?: authDataSource.currentUserId
            ?: return authTokenProvider.getCurrentAuthProvider()
        val existingUser = firestoreSyncDataSource.fetchUser(currentUserId)
        return existingUser?.authProvider?.takeIf { it != AuthProvider.UNKNOWN }
            ?: authTokenProvider.getCurrentAuthProvider()
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

    private suspend fun resolveUserFromSession(
        userId: String,
        email: String,
        displayName: String?,
        authProvider: AuthProvider
    ): User {
        val remoteUser = firestoreSyncDataSource.fetchUser(userId)

        if (remoteUser != null) {
            val normalizedProviderUser = if (remoteUser.authProvider == AuthProvider.UNKNOWN) {
                remoteUser.copy(authProvider = authProvider)
            } else {
                remoteUser
            }
            if (normalizedProviderUser != remoteUser) {
                runCatching { firestoreSyncDataSource.pushUser(normalizedProviderUser) }
            }
            authTokenProvider.setCachedSignupCompleted(normalizedProviderUser.signupCompleted)
            return applyLoginActivity(normalizeRoleIfNeeded(normalizedProviderUser))
        }

        val fallbackRole = resolveEffectiveRole(
            userId = userId,
            baseRole = UserRole.VISITOR
        )
        // Use cached signupCompleted if available (protects against transient Firestore errors
        // during sign-in for users who have previously completed signup).
        val signupCompleted = authTokenProvider.getCachedSignupCompleted()
            ?: (authProvider == AuthProvider.EMAIL)
        val createdUser = User(
            id = userId,
            email = email,
            nickname = resolveInitialNickname(email, displayName),
            profileImage = null,
            authProvider = authProvider,
            role = fallbackRole,
            banned = false,
            createdAt = nowIsoUtc(),
            signupCompleted = signupCompleted,
            lastLoginAt = nowIsoUtc()
        )
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
            val normalizedProviderUser = if (remoteUser.authProvider == AuthProvider.UNKNOWN) {
                remoteUser.copy(authProvider = authTokenProvider.getCurrentAuthProvider())
            } else {
                remoteUser
            }
            if (normalizedProviderUser != remoteUser) {
                runCatching { firestoreSyncDataSource.pushUser(normalizedProviderUser) }
            }
            authTokenProvider.setCachedSignupCompleted(normalizedProviderUser.signupCompleted)
            return applyLoginActivity(normalizeRoleIfNeeded(normalizedProviderUser))
        }

        val currentUserEmail = authTokenProvider.getCurrentUserEmail()

        if (currentUserEmail.isNullOrBlank()) {
            return null
        }
        val fallbackRole = resolveEffectiveRole(
            userId = currentUserId,
            baseRole = UserRole.VISITOR
        )
        // Use cached signupCompleted to avoid false redirect to sign-up when Firestore is
        // unreachable (e.g. offline). Falls back to auth-provider heuristic only when no
        // cached value exists (first-time social auth user who hasn't completed signup).
        val signupCompleted = authTokenProvider.getCachedSignupCompleted()
            ?: (authTokenProvider.getCurrentAuthProvider() == AuthProvider.EMAIL)
        val fallbackUser = User(
            id = currentUserId,
            email = currentUserEmail,
            nickname = resolveInitialNickname(currentUserEmail, null),
            profileImage = null,
            authProvider = authTokenProvider.getCurrentAuthProvider(),
            role = fallbackRole,
            banned = false,
            createdAt = nowIsoUtc(),
            signupCompleted = signupCompleted,
            lastLoginAt = nowIsoUtc()
        )
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
    } else emailPrefix.ifBlank {
        "유저"
    }
}

private fun resolveKakaoEmail(sessionEmail: String, profileEmail: String?): String {
    val normalizedProfileEmail = profileEmail?.trim().orEmpty()
    return normalizedProfileEmail.ifBlank {
        sessionEmail
    }
}

private fun resolveKakaoDisplayName(sessionDisplayName: String?, profileNickname: String?): String? {
    val normalizedProfileNickname = profileNickname?.trim().orEmpty()
    return normalizedProfileNickname.ifBlank {
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
