package com.hhp227.concafe.presentation.settings

import com.hhp227.concafe.presentation.theme.AppBrandTheme
import com.hhp227.concafe.presentation.theme.AppThemeMode

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appVersion: String = currentAppVersion(),
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val brandTheme: AppBrandTheme = AppBrandTheme.MAID_CAFE
) {
    companion object {
        fun empty(): SettingsUiState = SettingsUiState()
    }
}
