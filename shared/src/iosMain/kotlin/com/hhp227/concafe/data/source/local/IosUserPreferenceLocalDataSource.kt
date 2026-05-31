package com.hhp227.concafe.data.source.local

import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosUserPreferenceLocalDataSource : UserPreferenceLocalDataSource {
    private val defaults = NSUserDefaults.standardUserDefaults

    private val themeMode = MutableStateFlow(loadThemeMode())

    private val DetailTooltipType.preferenceKey: String
        get() = when (this) {
            DetailTooltipType.CAFE_FAVORITE -> KEY_CAFE_FAVORITE_TOOLTIP_SHOWN
            DetailTooltipType.CAST_FOLLOW -> KEY_CAST_FOLLOW_TOOLTIP_SHOWN
        }

    override fun observeThemeMode(): Flow<ThemeMode> {
        return themeMode.asStateFlow()
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        defaults.setObject(themeMode.name, forKey = KEY_THEME_MODE)
        defaults.synchronize()
        this.themeMode.value = themeMode
    }

    override fun hasShownDetailTooltip(type: DetailTooltipType): Boolean {
        return defaults.boolForKey(type.preferenceKey)
    }

    override fun markDetailTooltipShown(type: DetailTooltipType) {
        defaults.setBool(true, forKey = type.preferenceKey)
        defaults.synchronize()
    }

    private fun loadThemeMode(): ThemeMode {
        val storedValue = defaults.stringForKey(KEY_THEME_MODE)
            ?: defaults.stringForKey(LEGACY_KEY_THEME_MODE)
        return ThemeMode.entries.firstOrNull { it.name == storedValue }
            ?: ThemeMode.entries.firstOrNull { it.name.equals(storedValue, ignoreCase = true) }
            ?: ThemeMode.LIGHT
    }

    private companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val LEGACY_KEY_THEME_MODE = "concafe.theme.mode"
        private const val KEY_CAFE_FAVORITE_TOOLTIP_SHOWN = "cafe_favorite_tooltip_shown"
        private const val KEY_CAST_FOLLOW_TOOLTIP_SHOWN = "cast_follow_tooltip_shown"
    }
}
