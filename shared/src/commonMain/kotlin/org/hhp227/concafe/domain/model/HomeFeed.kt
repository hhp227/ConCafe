package org.hhp227.concafe.domain.model

data class HomeFeed(
    val banners: List<HomeBanner>,
    val popularCasts: List<HomePopularCast>,
    val nearbyCafes: List<HomeNearbyCafe>,
    val birthdayCasts: List<HomeBirthdayCast>,
    val notices: List<Notice>
)
