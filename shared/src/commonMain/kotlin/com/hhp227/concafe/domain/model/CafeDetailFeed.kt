package com.hhp227.concafe.domain.model

data class CafeDetailFeed(
    val detail: CafeDetail,
    val casts: List<CafeDetailCast>,
    val reviews: List<CafeDetailReview>,
    val isFavorite: Boolean,
    val isLoggedIn: Boolean
)
