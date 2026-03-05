package org.hhp227.concafe.presentation.navigation

import org.hhp227.concafe.presentation.navigation.Route

sealed interface NavigationEvent {
    data class NavigateTo(val route: Route) : NavigationEvent
    data object NavigateBack : NavigationEvent
}