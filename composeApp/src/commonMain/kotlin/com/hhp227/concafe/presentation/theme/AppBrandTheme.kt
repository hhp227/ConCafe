package com.hhp227.concafe.presentation.theme

import com.hhp227.concafe.domain.model.BrandTheme

enum class AppBrandTheme {
    MAID_CAFE,
    MENS_CON_CAFE
}

fun BrandTheme.toPresentationBrandTheme(): AppBrandTheme {
    return when (this) {
        BrandTheme.MAID_CAFE -> AppBrandTheme.MAID_CAFE
        BrandTheme.MENS_CON_CAFE -> AppBrandTheme.MENS_CON_CAFE
    }
}

fun AppBrandTheme.toDomainBrandTheme(): BrandTheme {
    return when (this) {
        AppBrandTheme.MAID_CAFE -> BrandTheme.MAID_CAFE
        AppBrandTheme.MENS_CON_CAFE -> BrandTheme.MENS_CON_CAFE
    }
}
