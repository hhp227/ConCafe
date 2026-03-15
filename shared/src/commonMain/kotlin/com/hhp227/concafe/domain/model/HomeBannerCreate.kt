package com.hhp227.concafe.domain.model

data class HomeBannerCreate(
    val cafeId: String?,
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val targetType: BannerLinkTargetType,
    val targetValue: String,
    val displayDays: Int
)
