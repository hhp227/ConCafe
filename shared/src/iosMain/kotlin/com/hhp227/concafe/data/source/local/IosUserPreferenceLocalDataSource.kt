package com.hhp227.concafe.data.source.local

import com.hhp227.concafe.domain.model.ContentLayout
import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosUserPreferenceLocalDataSource : UserPreferenceLocalDataSource {
    private val defaults = NSUserDefaults.standardUserDefaults

    private val themeMode = MutableStateFlow(loadThemeMode())

    private val brandTheme = MutableStateFlow(loadBrandTheme())

    private val contentLayout = MutableStateFlow(loadContentLayout())

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

    override fun observeBrandTheme(): Flow<BrandTheme> {
        return brandTheme.asStateFlow()
    }

    override fun setBrandTheme(brandTheme: BrandTheme) {
        defaults.setObject(brandTheme.name, forKey = KEY_BRAND_THEME)
        defaults.synchronize()
        this.brandTheme.value = brandTheme
    }

    override fun observeContentLayout(): Flow<ContentLayout> {
        return contentLayout.asStateFlow()
    }

    override fun setContentLayout(contentLayout: ContentLayout) {
        defaults.setObject(contentLayout.name, forKey = KEY_CONTENT_LAYOUT)
        defaults.synchronize()
        this.contentLayout.value = contentLayout
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

    private fun loadBrandTheme(): BrandTheme {
        val storedValue = defaults.stringForKey(KEY_BRAND_THEME)
        return BrandTheme.entries.firstOrNull { it.name == storedValue } ?: BrandTheme.MAID_CAFE
    }

    private fun loadContentLayout(): ContentLayout {
        val storedValue = defaults.stringForKey(KEY_CONTENT_LAYOUT)
        return ContentLayout.entries.firstOrNull { it.name == storedValue } ?: ContentLayout.FULL_BLEED
    }

    private companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BRAND_THEME = "brand_theme"
        private const val KEY_CONTENT_LAYOUT = "content_layout"
        private const val LEGACY_KEY_THEME_MODE = "concafe.theme.mode"
        private const val KEY_CAFE_FAVORITE_TOOLTIP_SHOWN = "cafe_favorite_tooltip_shown"
        private const val KEY_CAST_FOLLOW_TOOLTIP_SHOWN = "cast_follow_tooltip_shown"
    }
}
