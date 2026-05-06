package com.hhp227.concafe.presentation.theme

import kotlinx.coroutines.flow.StateFlow

interface ThemePreferenceStore {
    val themeMode: StateFlow<AppThemeMode>

    fun setThemeMode(themeMode: AppThemeMode)
}
