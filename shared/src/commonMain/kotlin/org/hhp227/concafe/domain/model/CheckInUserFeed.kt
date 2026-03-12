package com.hhp227.concafe.domain.model

data class CheckInUserFeed(
    val todayVisits: List<CheckInVisitEntry>,
    val recentVisits: List<CheckInVisitEntry>
)

data class CheckInVisitEntry(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val visitedAt: String,
    val visitedLabel: String,
    val memo: String?,
    val verified: Boolean
)
