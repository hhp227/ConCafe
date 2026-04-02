package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.UserRepository

class UserRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : UserRepository {
    override suspend fun getUser(userId: String): User {
        val remoteUser = firestoreSyncDataSource.fetchUser(userId)
            ?: throw NoSuchElementException("user not found")
        val replaced = authDataSource.replaceUser(remoteUser)

        if (!replaced) {
            authDataSource.addUser(remoteUser)
        }
        return remoteUser
    }

    override suspend fun updateProfile(userId: String, nickname: String, profileImage: String?) {
        val current = authDataSource.findUserById(userId)
        if (current == null) {
            throw NoSuchElementException("user not found")
        }

        val normalizedNickname = nickname.trim()
        val normalizedProfileImage = profileImage?.trim()?.ifBlank { null }

        firestoreSyncDataSource.updateUserProfile(
            userId = userId,
            nickname = normalizedNickname,
            profileImage = normalizedProfileImage
        )

        authDataSource.replaceUser(
            current.copy(
                nickname = normalizedNickname,
                profileImage = normalizedProfileImage
            )
        )
    }

    override suspend fun getMyPageSummary(userId: String): MyPageSummary {
        return firestoreSyncDataSource.fetchMyPageSummary(userId)
            ?: throw NoSuchElementException("my page summary not found")
    }
}
