package com.hhp227.concafe.domain.model

data class CastDetailFeed(
    val detail: CastDetail,
    val recentReviews: List<CastRecentReview>,
    val isFollowing: Boolean,
    val isLoggedIn: Boolean,
    val todayAttendanceStatus: CastAttendanceStatus = CastAttendanceStatus.OFF
)
