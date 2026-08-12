package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AdminUserFilter
import com.hhp227.concafe.domain.model.DormantAccountFilter
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.UserRepository
import kotlinx.datetime.Clock

class FakeUserRepository(
    private val dataSource: ConCafeDataSource
) : UserRepository {
    override suspend fun getUser(userId: String): User {
        return dataSource.users.firstOrNull { it.id == userId }
            ?: throw NoSuchElementException("user not found")
    }

    override suspend fun getAdminUserPage(
        filter: AdminUserFilter,
        cursor: String?,
        pageSize: Int
    ): PagedResult<User> {
        val filtered = dataSource.users
            .filter { user ->
                when (filter) {
                    AdminUserFilter.CAFE_OWNER -> user.role == UserRole.CAFE_OWNER
                    AdminUserFilter.BANNED -> user.banned
                }
            }
            .sortedByDescending { it.createdAt }
        val start = cursor?.let { value -> filtered.indexOfFirst { it.createdAt == value } + 1 }
            ?.takeIf { it > 0 }
            ?: 0
        val end = (start + pageSize.coerceAtLeast(1)).coerceAtMost(filtered.size)
        val items = filtered.subList(start, end)
        val nextCursor = if (end < filtered.size) items.lastOrNull()?.createdAt else null
        return PagedResult(items = items, nextCursor = nextCursor, hasNext = end < filtered.size)
    }

    override suspend fun getDormantAccountPage(
        filter: DormantAccountFilter,
        lastLoginBefore: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<User> {
        val filtered = if (filter == DormantAccountFilter.DORMANT) {
            dataSource.users.filter { it.dormant }.sortedByDescending { it.createdAt }
        } else {
            dataSource.users
                .filter { user ->
                    val lastLoginAt = user.lastLoginAt
                    !user.dormant && lastLoginAt != null && lastLoginAt <= lastLoginBefore
                }
                .sortedBy { it.lastLoginAt }
        }
        val cursorOf: (User) -> String? = if (filter == DormantAccountFilter.DORMANT) {
            { it.createdAt }
        } else {
            { it.lastLoginAt }
        }
        val start = cursor?.let { value -> filtered.indexOfFirst { cursorOf(it) == value } + 1 }
            ?.takeIf { it > 0 }
            ?: 0
        val end = (start + pageSize.coerceAtLeast(1)).coerceAtMost(filtered.size)
        val items = filtered.subList(start, end)
        val nextCursor = if (end < filtered.size) items.lastOrNull()?.let(cursorOf) else null
        return PagedResult(items = items, nextCursor = nextCursor, hasNext = end < filtered.size)
    }

    override suspend fun updateDormantStatus(userId: String, dormant: Boolean): User {
        val index = dataSource.users.indexOfFirst { it.id == userId }

        if (index == -1) {
            throw NoSuchElementException("user not found")
        } else {
            val current = dataSource.users[index]
            val dormantAt = if (dormant) Clock.System.now().toString() else null
            val updated = current.copy(dormant = dormant, dormantAt = dormantAt)
            dataSource.users[index] = updated
            return updated
        }
    }

    override suspend fun updateProfile(userId: String, nickname: String, profileImage: String?) {
        val index = dataSource.users.indexOfFirst { it.id == userId }

        if (index == -1) {
            throw NoSuchElementException("user not found")
        } else {
            val current = dataSource.users[index]
            dataSource.users[index] = current.copy(nickname = nickname, profileImage = profileImage)
        }
    }

    override suspend fun getMyPageSummary(userId: String): MyPageSummary {
        return dataSource.defaultMyPageSummary(userId)
    }
}
