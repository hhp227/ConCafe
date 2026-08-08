package com.hhp227.concafe.presentation.theme

import com.hhp227.concafe.domain.model.BannerLayout

enum class AppBannerLayout {
    FULL_BLEED,
    LEGACY
}

fun BannerLayout.toPresentationBannerLayout(): AppBannerLayout {
    return when (this) {
        BannerLayout.FULL_BLEED -> AppBannerLayout.FULL_BLEED
        BannerLayout.LEGACY -> AppBannerLayout.LEGACY
    }
}

fun AppBannerLayout.toDomainBannerLayout(): BannerLayout {
    return when (this) {
        AppBannerLayout.FULL_BLEED -> BannerLayout.FULL_BLEED
        AppBannerLayout.LEGACY -> BannerLayout.LEGACY
    }
}
