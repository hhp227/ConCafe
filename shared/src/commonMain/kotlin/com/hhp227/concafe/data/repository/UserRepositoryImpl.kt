package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.UserRepository

class UserRepositoryImpl(
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : UserRepository {
    override suspend fun getUser(userId: String): User {
        return firestoreSyncDataSource.fetchUser(userId)
            ?: throw NoSuchElementException("user not found")
    }

    override suspend fun updateProfile(userId: String, nickname: String, profileImage: String?) {
        val existingUser = firestoreSyncDataSource.fetchUser(userId)
        if (existingUser == null) {
            throw NoSuchElementException("user not found")
        }

        val normalizedNickname = nickname.trim()
        val normalizedProfileImage = profileImage?.trim()?.ifBlank { null }

        firestoreSyncDataSource.updateUserProfile(
            userId = userId,
            nickname = normalizedNickname,
            profileImage = normalizedProfileImage
        )
    }

    override suspend fun getMyPageSummary(userId: String): MyPageSummary {
        return firestoreSyncDataSource.fetchMyPageSummary(userId)
            ?: throw NoSuchElementException("my page summary not found")
    }
}
