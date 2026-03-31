package com.hhp227.concafe.presentation.navigation

sealed interface NavigationEvent {
    data class NavigateTo(val route: Route) : NavigationEvent
    data object NavigateBack : NavigationEvent
    data object RefreshUnreadNotificationCount : NavigationEvent
}