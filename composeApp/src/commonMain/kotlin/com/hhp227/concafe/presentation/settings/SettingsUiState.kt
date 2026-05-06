package com.hhp227.concafe.presentation.settings

import com.hhp227.concafe.presentation.theme.AppThemeMode

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appVersion: String = currentAppVersion(),
    val themeMode: AppThemeMode = AppThemeMode.LIGHT
) {
    companion object {
        fun empty(): SettingsUiState = SettingsUiState()
    }
}
