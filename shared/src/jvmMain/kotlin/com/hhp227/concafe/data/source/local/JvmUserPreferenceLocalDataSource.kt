package com.hhp227.concafe.data.source.local

import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import java.util.prefs.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class JvmUserPreferenceLocalDataSource : UserPreferenceLocalDataSource {
    private val preferences = Preferences.userRoot().node(PREF_NODE)

    private val themeMode = MutableStateFlow(loadThemeMode())

    private val brandTheme = MutableStateFlow(loadBrandTheme())

    private val DetailTooltipType.preferenceKey: String
        get() = when (this) {
            DetailTooltipType.CAFE_FAVORITE -> KEY_CAFE_FAVORITE_TOOLTIP_SHOWN
            DetailTooltipType.CAST_FOLLOW -> KEY_CAST_FOLLOW_TOOLTIP_SHOWN
        }

    override fun observeThemeMode(): Flow<ThemeMode> {
        return themeMode.asStateFlow()
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        preferences.put(KEY_THEME_MODE, themeMode.name)
        this.themeMode.value = themeMode
    }

    override fun observeBrandTheme(): Flow<BrandTheme> {
        return brandTheme.asStateFlow()
    }

    override fun setBrandTheme(brandTheme: BrandTheme) {
        preferences.put(KEY_BRAND_THEME, brandTheme.name)
        this.brandTheme.value = brandTheme
    }

    override fun hasShownDetailTooltip(type: DetailTooltipType): Boolean {
        return preferences.getBoolean(type.preferenceKey, false)
    }

    override fun markDetailTooltipShown(type: DetailTooltipType) {
        preferences.putBoolean(type.preferenceKey, true)
    }

    private fun loadThemeMode(): ThemeMode {
        val storedValue = preferences.get(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == storedValue } ?: ThemeMode.LIGHT
    }

    private fun loadBrandTheme(): BrandTheme {
        val storedValue = preferences.get(KEY_BRAND_THEME, null)
        return BrandTheme.entries.firstOrNull { it.name == storedValue } ?: BrandTheme.MAID_CAFE
    }

    private companion object {
        private const val PREF_NODE = "com/hhp227/concafe/theme"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BRAND_THEME = "brand_theme"
        private const val KEY_CAFE_FAVORITE_TOOLTIP_SHOWN = "cafe_favorite_tooltip_shown"
        private const val KEY_CAST_FOLLOW_TOOLTIP_SHOWN = "cast_follow_tooltip_shown"
    }
}
