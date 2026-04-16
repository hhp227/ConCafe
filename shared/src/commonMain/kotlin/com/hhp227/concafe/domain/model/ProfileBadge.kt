package com.hhp227.concafe.domain.model

data class ProfileBadge(
    val id: String,
    val name: String,
    val icon: String,
    val unlocked: Boolean,
    val currentCount: Int = 0,
    val goalCount: Int = 1
)
