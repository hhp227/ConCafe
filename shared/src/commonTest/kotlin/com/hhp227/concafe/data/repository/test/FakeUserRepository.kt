package com.hhp227.concafe.data.repository.test

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.AdminUserFilter
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.UserRepository

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
