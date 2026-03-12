package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardAction {
    data object ClickBack : CafeDashboardAction
    data class ClickShortcut(val shortcut: CafeDashboardShortcut) : CafeDashboardAction
    data class ClickCastSchedule(val castId: String) : CafeDashboardAction
    data object ClickLoadMoreCasts : CafeDashboardAction
    data object DismissInfoMessage : CafeDashboardAction
}
