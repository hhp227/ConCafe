package org.hhp227.concafe.presentation.navigation

sealed interface NavigationAction {
    data class NavigateToMain(val initialTab: String? = null) : NavigationAction
    data class NavigateToCast(val id: String) : NavigationAction
    data class NavigateToCafe(val id: String) : NavigationAction
    data object NavigateToSignIn : NavigationAction
    data object NavigateToSignUp : NavigationAction
    data object NavigateToNotification : NavigationAction
    data object NavigateToSettings : NavigationAction
    data object NavigateBack : NavigationAction
}
