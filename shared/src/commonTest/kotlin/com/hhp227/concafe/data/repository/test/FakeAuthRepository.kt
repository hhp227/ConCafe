package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.AuthProvider
import com.hhp227.concafe.domain.model.DeleteAccountRequest
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
                authProvider = AuthProvider.EMAIL,
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

    override suspend fun signInWithGoogleIdToken(idToken: String): User {
        if (idToken.isBlank()) {
            throw IllegalArgumentException("google idToken is required")
        }

        val email = "google-user@concafe.test"
        val found = dataSource.users.firstOrNull { it.email == email }
        return if (found != null) {
            dataSource.currentUserId = found.id
            currentUserFlow.value = found
            found
        } else {
            val created = User(
                id = "user-${dataSource.users.size + 1}",
                email = email,
                nickname = "구글유저",
                profileImage = null,
                authProvider = AuthProvider.GOOGLE,
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

    override suspend fun signInWithAppleIdToken(idToken: String): User {
        if (idToken.isBlank()) {
            throw IllegalArgumentException("apple idToken is required")
        }

        val email = "apple-user@concafe.test"
        val found = dataSource.users.firstOrNull { it.email == email }
        return if (found != null) {
            dataSource.currentUserId = found.id
            currentUserFlow.value = found
            found
        } else {
            val created = User(
                id = "user-${dataSource.users.size + 1}",
                email = email,
                nickname = "애플유저",
                profileImage = null,
                authProvider = AuthProvider.APPLE,
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

    override suspend fun signInWithKakaoIdToken(
        idToken: String,
        email: String?,
        nickname: String?
    ): User {
        if (idToken.isBlank()) {
            throw IllegalArgumentException("kakao idToken is required")
        }

        val resolvedEmail = if (email.isNullOrBlank()) "kakao-user@concafe.test" else email
        val resolvedNickname = if (nickname.isNullOrBlank()) "카카오유저" else nickname
        val found = dataSource.users.firstOrNull { it.email == resolvedEmail }
        return if (found != null) {
            dataSource.currentUserId = found.id
            currentUserFlow.value = found
            found
        } else {
            val created = User(
                id = "user-${dataSource.users.size + 1}",
                email = resolvedEmail,
                nickname = resolvedNickname,
                profileImage = null,
                authProvider = AuthProvider.KAKAO,
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
            authProvider = AuthProvider.EMAIL,
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

    override suspend fun completeSignUpForCurrentUser(
        email: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String?,
        phoneNumber: String?
    ): User {
        val currentUserId = dataSource.currentUserId
            ?: throw IllegalArgumentException("no signed in user")
        val currentUserIndex = dataSource.users.indexOfFirst { user -> user.id == currentUserId }

        if (currentUserIndex < 0) {
            throw IllegalArgumentException("current user not found")
        }
        val currentUser = dataSource.users[currentUserIndex]
        val updatedUser = currentUser.copy(
            email = email,
            nickname = nickname,
            authProvider = currentUser.authProvider,
            role = role,
            phoneNumber = phoneNumber
        )

        dataSource.users[currentUserIndex] = updatedUser
        if (role == UserRole.CAST && !affiliatedCafeId.isNullOrBlank()) {
            dataSource.affiliatedCafeIdByUser[updatedUser.id] = affiliatedCafeId
        }
        currentUserFlow.value = updatedUser
        return updatedUser
    }

    override suspend fun signOut() {
        dataSource.currentUserId = null
        currentUserFlow.value = null
    }

    override suspend fun requestPasswordReset(email: String) {
        if (email.isBlank()) {
            throw IllegalArgumentException("email is required")
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            throw IllegalArgumentException("currentPassword/newPassword is required")
        }
        if (dataSource.currentUserId == null) {
            throw IllegalArgumentException("no signed in user")
        }
    }

    override suspend fun deleteAccount(request: DeleteAccountRequest) {
        if (request.provider == AuthProvider.EMAIL && request.password.isNullOrBlank()) {
            throw IllegalArgumentException("password is required")
        }

        val currentUserId = dataSource.currentUserId ?: return
        val userIndex = dataSource.users.indexOfFirst { it.id == currentUserId }

        if (userIndex >= 0) {
            dataSource.users.removeAt(userIndex)
        }

        dataSource.currentUserId = null
        currentUserFlow.value = null
    }

    override suspend fun getCurrentAuthProvider(): AuthProvider {
        val currentUserId = dataSource.currentUserId ?: return AuthProvider.UNKNOWN
        return dataSource.users.firstOrNull { it.id == currentUserId }?.authProvider ?: AuthProvider.UNKNOWN
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
