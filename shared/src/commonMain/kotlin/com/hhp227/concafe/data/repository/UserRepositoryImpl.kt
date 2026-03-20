package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.AuthDataSource
import com.hhp227.concafe.data.source.MyInfoDataSource
import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.UserRepository

class UserRepositoryImpl(
    private val authDataSource: AuthDataSource,
    private val myInfoDataSource: MyInfoDataSource
) : UserRepository {
    override suspend fun getUser(userId: String): User {
        return authDataSource.findUserById(userId)
            ?: throw NoSuchElementException("user not found")
    }

    override suspend fun updateProfile(userId: String, nickname: String, profileImage: String?) {
        val current = authDataSource.findUserById(userId)
        if (current == null) {
            throw NoSuchElementException("user not found")
        }

        authDataSource.replaceUser(current.copy(nickname = nickname, profileImage = profileImage))
    }

    override suspend fun getMyPageSummary(userId: String): MyPageSummary {
        return myInfoDataSource.defaultMyPageSummary(userId)
    }
}