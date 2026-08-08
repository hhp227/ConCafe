package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.BannerLayout
import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferenceRepository {
    fun observeThemeMode(): Flow<ThemeMode>

    fun setThemeMode(themeMode: ThemeMode)

    fun observeBrandTheme(): Flow<BrandTheme>

    fun setBrandTheme(brandTheme: BrandTheme)

    fun observeBannerLayout(): Flow<BannerLayout>

    fun setBannerLayout(bannerLayout: BannerLayout)

    fun hasShownDetailTooltip(type: DetailTooltipType): Boolean

    fun markDetailTooltipShown(type: DetailTooltipType)
}
