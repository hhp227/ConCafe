package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardEvent {
    data object NavigateBack : CafeDashboardEvent
    data object NavigateToBannerEdit : CafeDashboardEvent
    data class NavigateToCafeInfoEdit(val cafeId: String) : CafeDashboardEvent
    data class NavigateToNoticeEvent(val cafeId: String) : CafeDashboardEvent
    data class NavigateToMenuGoods(val cafeId: String) : CafeDashboardEvent
    data class NavigateToCastEdit(val cafeId: String, val castId: String? = null) : CafeDashboardEvent
    data class NavigateToSchedule(val castId: String? = null) : CafeDashboardEvent
    data class NavigateToExternalLink(val title: String, val url: String) : CafeDashboardEvent
}
