package com.hhp227.concafe.domain.model

data class HomeFeed(
    val banners: List<HomeBanner>,
    val popularCasts: List<Cast>,
    val popularCastCafeNames: Map<String, String>,
    val popularCastsNextCursor: String?,
    val hasMorePopularCasts: Boolean,
    val nearbyCafes: List<Cafe>,
    val nearbyCafesNextCursor: String?,
    val hasMoreNearbyCafes: Boolean,
    val birthdayCasts: List<Cast>,
    val notices: List<Notice>,
    val cafeEvents: List<HomeCafeEvent>
)

data class HomeCafeEvent(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val title: String,
    val content: String,
    val imageUrl: String,
    val periodText: String,
    val statusLabel: String
)
