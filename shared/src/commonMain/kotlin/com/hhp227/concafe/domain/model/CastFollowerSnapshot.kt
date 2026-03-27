package com.hhp227.concafe.domain.model

data class CastFollowerSnapshot(
    val userId: String,
    val followedAt: String,
    val userNickname: String? = null,
    val userProfileImage: String? = null
)
