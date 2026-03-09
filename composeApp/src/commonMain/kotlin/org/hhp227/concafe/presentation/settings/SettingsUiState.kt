package org.hhp227.concafe.presentation.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun empty(): SettingsUiState = SettingsUiState()
    }
}
