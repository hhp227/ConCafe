package org.hhp227.concafe.domain.model

data class HomeFeed(
    val banners: List<HomeBanner>,
    val popularCasts: List<Cast>,
    val nearbyCafes: List<Cafe>,
    val birthdayCasts: List<Cast>,
    val notices: List<Notice>
)
