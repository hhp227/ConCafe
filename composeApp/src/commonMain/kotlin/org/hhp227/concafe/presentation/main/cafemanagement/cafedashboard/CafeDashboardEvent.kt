package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardEvent {
    data object NavigateBack : CafeDashboardEvent
    data class NavigateToCafeInfoEdit(val cafeId: String) : CafeDashboardEvent
    data class NavigateToMenuGoods(val cafeId: String) : CafeDashboardEvent
    data class NavigateToCastEdit(val cafeId: String, val castId: String? = null) : CafeDashboardEvent
    data object NavigateToSchedule : CafeDashboardEvent
}
