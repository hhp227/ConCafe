package com.hhp227.concafe.data.source.local

import android.content.Context
import androidx.core.content.edit
import com.hhp227.concafe.domain.model.ContentLayout
import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidUserPreferenceLocalDataSource(context: Context) : UserPreferenceLocalDataSource {
    private val preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val themeMode = MutableStateFlow(loadThemeMode())

    private val brandTheme = MutableStateFlow(loadBrandTheme())

    private val contentLayout = MutableStateFlow(loadContentLayout())

    override fun observeThemeMode(): Flow<ThemeMode> {
        return themeMode.asStateFlow()
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, themeMode.name) }
        this.themeMode.value = themeMode
    }

    override fun observeBrandTheme(): Flow<BrandTheme> {
        return brandTheme.asStateFlow()
    }

    override fun setBrandTheme(brandTheme: BrandTheme) {
        preferences.edit { putString(KEY_BRAND_THEME, brandTheme.name) }
        this.brandTheme.value = brandTheme
    }

    override fun observeContentLayout(): Flow<ContentLayout> {
        return contentLayout.asStateFlow()
    }

    override fun setContentLayout(contentLayout: ContentLayout) {
        preferences.edit { putString(KEY_CONTENT_LAYOUT, contentLayout.name) }
        this.contentLayout.value = contentLayout
    }

    override fun hasShownDetailTooltip(type: DetailTooltipType): Boolean {
        return preferences.getBoolean(type.preferenceKey, false)
    }

    override fun markDetailTooltipShown(type: DetailTooltipType) {
        preferences.edit { putBoolean(type.preferenceKey, true) }
    }

    private fun loadThemeMode(): ThemeMode {
        val storedValue = preferences.getString(KEY_THEME_MODE, null)
        return ThemeMode.entries.firstOrNull { it.name == storedValue } ?: ThemeMode.LIGHT
    }

    private fun loadBrandTheme(): BrandTheme {
        val storedValue = preferences.getString(KEY_BRAND_THEME, null)
        return BrandTheme.entries.firstOrNull { it.name == storedValue } ?: BrandTheme.MAID_CAFE
    }

    private fun loadContentLayout(): ContentLayout {
        val storedValue = preferences.getString(KEY_CONTENT_LAYOUT, null)
        return ContentLayout.entries.firstOrNull { it.name == storedValue } ?: ContentLayout.FULL_BLEED
    }

    private val DetailTooltipType.preferenceKey: String
        get() = when (this) {
            DetailTooltipType.CAFE_FAVORITE -> KEY_CAFE_FAVORITE_TOOLTIP_SHOWN
            DetailTooltipType.CAST_FOLLOW -> KEY_CAST_FOLLOW_TOOLTIP_SHOWN
        }

    private companion object {
        private const val PREF_NAME = "concafe.theme.preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BRAND_THEME = "brand_theme"
        private const val KEY_CONTENT_LAYOUT = "content_layout"
        private const val KEY_CAFE_FAVORITE_TOOLTIP_SHOWN = "cafe_favorite_tooltip_shown"
        private const val KEY_CAST_FOLLOW_TOOLTIP_SHOWN = "cast_follow_tooltip_shown"
    }
}
