package com.hhp227.concafe.presentation.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appVersion: String = currentAppVersion()
) {
    companion object {
        fun empty(): SettingsUiState = SettingsUiState()
    }
}
