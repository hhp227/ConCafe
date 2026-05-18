package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.UserBlock
import com.hhp227.concafe.domain.model.UserBlockCreate

interface UserBlockRemoteDataSource {
    suspend fun createUserBlock(
        blockerUserId: String,
        blockerNickname: String,
        input: UserBlockCreate
    ): UserBlock
}
