package com.hhp227.concafe.presentation.settings.account

sealed interface AccountSettingsEvent {
    data object NavigateBack : AccountSettingsEvent
    data class NavigateToCastEdit(val cafeId: String?, val castId: String?) : AccountSettingsEvent
    data object NavigateToChangePassword : AccountSettingsEvent
    data class ShowMessage(val message: String) : AccountSettingsEvent
}
