package com.hhp227.concafe.data.source.local

import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferenceLocalDataSource {
    fun observeThemeMode(): Flow<ThemeMode>

    fun setThemeMode(themeMode: ThemeMode)

    fun hasShownDetailTooltip(type: DetailTooltipType): Boolean

    fun markDetailTooltipShown(type: DetailTooltipType)
}
