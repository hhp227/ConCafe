package com.hhp227.concafe.presentation.navigation

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
    data class Banner(val cafeId: String? = null) : Route

    @Serializable
    data class BannerEdit(
        val cafeId: String? = null,
        val bannerId: String? = null
    ) : Route

    @Serializable
    data class ExternalLink(val title: String, val url: String) : Route

    @Serializable
    data class CafeInfoEdit(
        val param: String? = null,
        val isRegistrationMode: Boolean = false
    ) : Route

    @Serializable
    data class NoticeEvent(val param: String) : Route

    @Serializable
    data class CastEdit(val cafeId: String? = null, val castId: String? = null) : Route

    @Serializable
    data class Schedule(val castId: String? = null) : Route

    @Serializable
    data class MenuGoods(val param: String) : Route

    @Serializable
    data class MenuGoodsEdit(
        val cafeId: String,
        val itemId: String? = null
    ) : Route

    @Serializable
    data class ReviewEdit(val cafeId: String, val reviewId: String? = null) : Route

    @Serializable
    data class Picture(val imageUrl: String) : Route

    @Serializable
    data object SignIn : Route

    @Serializable
    data object SignUp : Route

    @Serializable
    data object ResetPassword : Route

    @Serializable
    data object Notification : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object NotificationSettings : Route

    @Serializable
    data object AccountSettings : Route

    @Serializable
    data object Inquiry : Route

    @Serializable
    data object ChangePassword : Route
}
