package com.hhp227.concafe.domain.model

data class HomeBanner(
    val id: String,
    val title: String,
    val startColorHex: String,
    val endColorHex: String,
    val subtitle: String = "",
    val cafeId: String? = null,
    val imageUrl: String? = null,
    val targetType: BannerLinkTargetType = BannerLinkTargetType.EXTERNAL_LINK,
    val targetValue: String = "",
    val displayDays: Int = 1,
    val statusLabel: String = "ACTIVE",
    val createdAtEpochMillis: Long = 0L,
    val activatedAtEpochMillis: Long = 0L
)
