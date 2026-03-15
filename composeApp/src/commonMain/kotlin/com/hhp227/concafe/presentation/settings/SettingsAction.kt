package com.hhp227.concafe.presentation.settings

sealed interface SettingsAction {
    data object ClickBack : SettingsAction
    data object ClickPrivacyPolicy : SettingsAction
    data object ClickSignOut : SettingsAction
}
