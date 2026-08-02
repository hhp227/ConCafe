package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.firestore.FirestoreSyncDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AdminUserFilter
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.UserRepository
import kotlinx.datetime.Clock

class UserRepositoryImpl(
    private val firestoreSyncDataSource: FirestoreSyncDataSource
) : UserRepository {
    override suspend fun getUser(userId: String): User {
        return firestoreSyncDataSource.fetchUser(userId)
            ?: throw NoSuchElementException("user not found")
    }

    override suspend fun getAdminUserPage(
        filter: AdminUserFilter,
        cursor: String?,
        pageSize: Int
    ): PagedResult<User> {
        return firestoreSyncDataSource.fetchAdminUserPage(
            filter = filter,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun getDormantAccountPage(
        filter: DormantAccountFilter,
        lastLoginBefore: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<User> {
        return firestoreSyncDataSource.fetchDormantAccountPage(
            filter = filter,
            lastLoginBefore = lastLoginBefore,
            cursor = cursor,
            pageSize = pageSize
        )
    }

    override suspend fun updateDormantStatus(userId: String, dormant: Boolean): User {
        val existingUser = firestoreSyncDataSource.fetchUser(userId)
            ?: throw NoSuchElementException("user not found")
        val dormantAt = if (dormant) Clock.System.now().toString() else null

        firestoreSyncDataSource.updateUserDormantStatus(
            userId = userId,
            dormant = dormant,
            dormantAt = dormantAt
        )
        return existingUser.copy(dormant = dormant, dormantAt = dormantAt)
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
