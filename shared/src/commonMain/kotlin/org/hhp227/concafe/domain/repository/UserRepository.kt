package org.hhp227.concafe.domain.repository

import org.hhp227.concafe.domain.model.MyPageSummary
import org.hhp227.concafe.domain.model.User

interface UserRepository {
    suspend fun getUser(userId: String): User

    suspend fun updateProfile(userId: String, nickname: String, profileImage: String?)

    suspend fun getMyPageSummary(userId: String): MyPageSummary
}
