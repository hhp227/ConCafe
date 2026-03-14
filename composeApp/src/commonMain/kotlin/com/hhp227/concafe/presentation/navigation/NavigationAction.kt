package com.hhp227.concafe.presentation.navigation

sealed interface NavigationAction {
    data class NavigateToMain(val initialTab: String? = null) : NavigationAction
    data class NavigateToCast(val id: String) : NavigationAction
    data class NavigateToCafe(val id: String) : NavigationAction
    data class NavigateToCafeDashboard(val id: String) : NavigationAction
    data object NavigateToBannerEdit : NavigationAction
    data class NavigateToCafeInfoEdit(
        val id: String? = null,
        val isRegistrationMode: Boolean = false
    ) : NavigationAction
    data class NavigateToNoticeEvent(val id: String) : NavigationAction
    data class NavigateToCastEdit(val cafeId: String? = null, val castId: String? = null) : NavigationAction
    data class NavigateToSchedule(val castId: String? = null) : NavigationAction
    data class NavigateToMenuGoods(val id: String) : NavigationAction
    data class NavigateToMenuGoodsEdit(val cafeId: String, val itemId: String? = null) : NavigationAction
    data class NavigateToReviewEdit(val cafeId: String) : NavigationAction
    data object NavigateToSignIn : NavigationAction
    data object NavigateToSignUp : NavigationAction
    data object NavigateToNotification : NavigationAction
    data object NavigateToSettings : NavigationAction
    data object NavigateBack : NavigationAction
}
