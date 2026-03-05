package org.hhp227.concafe.presentation.navigation

sealed interface NavigationAction {
    data class NavigateToMain(val initialTab: String? = null) : NavigationAction
    data class NavigateToDetail(val id: String) : NavigationAction
    data object NavigateBack : NavigationAction
}