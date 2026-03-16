package com.hhp227.concafe.presentation.settings

sealed interface SettingsAction {
    data object ClickBack : SettingsAction
    data object ClickAccountSettings : SettingsAction
    data object ClickNotificationSettings : SettingsAction
    data object ClickCustomerSupport : SettingsAction
    data object ClickInquiry : SettingsAction
    data object ClickPrivacyPolicy : SettingsAction
    data object ClickSignOut : SettingsAction
}
