package com.hhp227.concafe.domain.model

data class FanManagementData(
    val user: User,
    val detail: CastDetail,
    val followers: List<FanFollower>
)

data class FanFollower(
    val id: String,
    val nickname: String,
    val profileImage: String?,
    val followedAt: String
)
