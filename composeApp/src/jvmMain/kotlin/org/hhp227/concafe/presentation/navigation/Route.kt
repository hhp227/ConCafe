package org.hhp227.concafe.presentation.navigation

sealed interface Route {
    data object Entry : Route
    data class Main(val initialTab: String? = null) : Route
    data class CastDetail(val param: String) : Route
    data class CafeDetail(val param: String) : Route
    data object Notification : Route
}
