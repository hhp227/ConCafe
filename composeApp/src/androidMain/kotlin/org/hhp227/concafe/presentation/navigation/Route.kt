package org.hhp227.concafe.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Entry : Route

    @Serializable
    data class Main(val initialTab: String? = null) : Route

    @Serializable
    data class Detail(val param: String) : Route

    @Serializable
    data object Notification : Route
}
