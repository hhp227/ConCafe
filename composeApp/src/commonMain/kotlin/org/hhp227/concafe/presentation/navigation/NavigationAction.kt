package org.hhp227.concafe.presentation.navigation

sealed interface NavigationAction {
    data class NavigateToMain(val initialTab: String? = null) : NavigationAction
    data class NavigateToCastDetail(val id: String) : NavigationAction

    data class NavigateToCafeDetail(val id: String) : NavigationAction
    data object NavigateToNotification : NavigationAction
    data object NavigateBack : NavigationAction
}