package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.local.UserPreferenceLocalDataSource
import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import com.hhp227.concafe.domain.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.Flow

class UserPreferenceRepositoryImpl(
    private val localDataSource: UserPreferenceLocalDataSource
) : UserPreferenceRepository {
    override fun observeThemeMode(): Flow<ThemeMode> {
        return localDataSource.observeThemeMode()
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        localDataSource.setThemeMode(themeMode)
    }

    override fun observeBrandTheme(): Flow<BrandTheme> {
        return localDataSource.observeBrandTheme()
    }

    override fun setBrandTheme(brandTheme: BrandTheme) {
        localDataSource.setBrandTheme(brandTheme)
    }

    override fun hasShownDetailTooltip(type: DetailTooltipType): Boolean {
        return localDataSource.hasShownDetailTooltip(type)
    }

    override fun markDetailTooltipShown(type: DetailTooltipType) {
        localDataSource.markDetailTooltipShown(type)
    }
}
