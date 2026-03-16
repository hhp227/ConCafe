package com.hhp227.concafe.domain.model

data class MyPageSummary(
    val userId: String,
    val totalVisits: Int,
    val favoritesCount: Int,
    val followedCastsCount: Int,
    val badgesCount: Int,
    val level: Int
)
