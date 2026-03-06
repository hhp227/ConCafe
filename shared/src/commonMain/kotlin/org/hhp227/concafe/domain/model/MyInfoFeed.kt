package org.hhp227.concafe.domain.model

data class MyInfoFeed(
    val isLoggedIn: Boolean,
    val user: User?,
    val summary: MyPageSummary?,
    val badges: List<ProfileBadge>,
    val popularCafes: List<Cafe>,
    val recentVisits: List<Cafe>,
    val favorites: List<Cafe>,
    val followedMaids: List<Cast>
)
