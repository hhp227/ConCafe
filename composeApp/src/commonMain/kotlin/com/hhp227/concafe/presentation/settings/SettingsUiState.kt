package com.hhp227.concafe.presentation.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appVersion: String = APP_VERSION
) {
    companion object {
        private const val APP_VERSION = "1.0.0"

        fun empty(): SettingsUiState = SettingsUiState()
    }
}
