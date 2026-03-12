package com.hhp227.concafe.presentation.settings

sealed interface SettingsAction {
    data object ClickBack : SettingsAction

    data object ClickSignOut : SettingsAction
}
