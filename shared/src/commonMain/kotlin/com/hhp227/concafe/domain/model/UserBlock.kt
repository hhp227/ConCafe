package com.hhp227.concafe.domain.model

data class UserBlock(
    val id: String,
    val blockerUserId: String,
    val blockerNickname: String,
    val blockedUserId: String,
    val blockedNickname: String,
    val createdAt: String
)

data class UserBlockCreate(
    val blockedUserId: String,
    val blockedNickname: String
)
