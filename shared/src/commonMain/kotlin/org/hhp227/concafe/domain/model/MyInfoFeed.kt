package org.hhp227.concafe.domain.model

data class MyInfoFeed(
    val isLoggedIn: Boolean,
    val user: User?,
    val summary: MyPageSummary?,
    val castDetail: CastDetail?,
    val ownedCafes: List<CafeManagementData.OwnedCafeSummary>,
    val badges: List<ProfileBadge>,
    val popularCafes: List<Cafe>,
    val recentVisits: List<Cafe>,
    val favorites: List<Cafe>,
    val followedMaids: List<Cast>
)
