package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.MyPageSummary
import com.hhp227.concafe.domain.model.User

interface UserRepository {
    suspend fun getUser(userId: String): User

    suspend fun updateProfile(userId: String, nickname: String, profileImage: String?)

    suspend fun getMyPageSummary(userId: String): MyPageSummary
}
