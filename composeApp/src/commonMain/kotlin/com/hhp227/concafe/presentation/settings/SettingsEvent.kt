package com.hhp227.concafe.presentation.settings

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
    data object NavigateToAccountSettings : SettingsEvent
    data object NavigateToNotificationSettings : SettingsEvent
    data object NavigateToInquiryLink : SettingsEvent
    data class NavigateToExternalLink(val title: String, val url: String) : SettingsEvent
}
