package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.UserBlock
import com.hhp227.concafe.domain.model.UserBlockCreate

interface UserBlockRepository {
    suspend fun createUserBlock(
        blockerUserId: String,
        blockerNickname: String,
        input: UserBlockCreate
    ): UserBlock
}
