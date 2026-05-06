package com.hhp227.concafe.presentation.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidThemePreferenceStore(context: Context) : ThemePreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(themeMode: AppThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _themeMode.value = themeMode
    }

    private fun loadThemeMode(): AppThemeMode {
        val storedValue = preferences.getString(KEY_THEME_MODE, null)
        return AppThemeMode.entries.firstOrNull { it.name == storedValue } ?: AppThemeMode.LIGHT
    }

    private companion object {
        private const val PREF_NAME = "concafe.theme.preferences"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
