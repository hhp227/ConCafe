package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.UserBlockRemoteDataSource
import com.hhp227.concafe.domain.model.UserBlock
import com.hhp227.concafe.domain.model.UserBlockCreate
import com.hhp227.concafe.domain.repository.UserBlockRepository

class UserBlockRepositoryImpl(
    private val userBlockRemoteDataSource: UserBlockRemoteDataSource
) : UserBlockRepository {
    override suspend fun createUserBlock(
        blockerUserId: String,
        blockerNickname: String,
        input: UserBlockCreate
    ): UserBlock {
        if (blockerUserId.isBlank()) throw IllegalArgumentException("로그인이 필요합니다.")
        if (input.blockedUserId.isBlank()) throw IllegalArgumentException("차단할 사용자가 올바르지 않습니다.")
        if (blockerUserId == input.blockedUserId) throw IllegalArgumentException("본인은 차단할 수 없습니다.")
        return userBlockRemoteDataSource.createUserBlock(
            blockerUserId = blockerUserId,
            blockerNickname = blockerNickname,
            input = input
        )
    }
}
