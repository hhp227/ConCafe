package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.UserRepository

class UserRepositoryImpl : UserRepository {
    override suspend fun getUser(userId: String): User {
        TODO("Not yet implemented")
    }

    override suspend fun updateProfile(userId: String, nickname: String, profileImage: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun getMyPageSummary(userId: String): MyPageSummary {
        TODO("Not yet implemented")
    }
}