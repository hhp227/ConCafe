package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.ConCafeDataSource
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.UserRepository

class FakeUserRepository(
    private val dataSource: ConCafeDataSource
) : UserRepository {
    override suspend fun getUser(userId: String): User {
        return dataSource.users.firstOrNull { it.id == userId }
            ?: throw NoSuchElementException("user not found")
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
