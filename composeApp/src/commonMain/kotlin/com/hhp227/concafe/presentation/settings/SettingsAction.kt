package com.hhp227.concafe.presentation.settings

import com.hhp227.concafe.presentation.theme.AppContentLayout
import com.hhp227.concafe.presentation.theme.AppBrandTheme
import com.hhp227.concafe.presentation.theme.AppThemeMode

sealed interface SettingsAction {
    data object ClickBack : SettingsAction
    data object ClickAccountSettings : SettingsAction
    data object ClickNotificationSettings : SettingsAction
    data object ClickCustomerSupport : SettingsAction
    data object ClickInquiry : SettingsAction
    data object ClickPrivacyPolicy : SettingsAction
    data object ClickSignOut : SettingsAction
    data class SelectThemeMode(val themeMode: AppThemeMode) : SettingsAction
    data class SelectBrandTheme(val brandTheme: AppBrandTheme) : SettingsAction
    data class SelectContentLayout(val contentLayout: AppContentLayout) : SettingsAction
}
