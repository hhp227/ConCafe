package com.hhp227.concafe.presentation.theme

import java.util.prefs.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JvmThemePreferenceStore : ThemePreferenceStore {
    private val preferences = Preferences.userRoot().node(PREF_NODE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    override fun setThemeMode(themeMode: AppThemeMode) {
        preferences.put(KEY_THEME_MODE, themeMode.name)
        _themeMode.value = themeMode
    }

    private fun loadThemeMode(): AppThemeMode {
        val storedValue = preferences.get(KEY_THEME_MODE, null)
        return AppThemeMode.entries.firstOrNull { it.name == storedValue } ?: AppThemeMode.LIGHT
    }

    private companion object {
        private const val PREF_NODE = "com/hhp227/concafe/theme"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
