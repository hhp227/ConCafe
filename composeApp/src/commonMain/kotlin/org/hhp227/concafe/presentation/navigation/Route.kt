package org.hhp227.concafe.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Entry : Route

    @Serializable
    data class Main(val initialTab: String? = null) : Route

    @Serializable
    data class Cast(val param: String) : Route

    @Serializable
    data class Cafe(val param: String) : Route

    @Serializable
    data class CafeDashboard(val param: String) : Route

    @Serializable
    data class CafeInfoEdit(val param: String) : Route

    @Serializable
    data object SignIn : Route

    @Serializable
    data object SignUp : Route

    @Serializable
    data object Notification : Route

    @Serializable
    data object Settings : Route
}
