package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardAction {
    data object ClickBack : CafeDashboardAction
    data class ClickShortcut(val shortcut: CafeDashboardShortcut) : CafeDashboardAction
    data class ClickCastSchedule(val castId: String) : CafeDashboardAction
    data object ClickDeleteCast : CafeDashboardAction
    data object ConfirmDeleteCast : CafeDashboardAction
    data object DismissDeleteCastDialog : CafeDashboardAction
    data class ClickApproveCastClaim(val claimId: String) : CafeDashboardAction
    data class ClickRejectCastClaim(val claimId: String) : CafeDashboardAction
    data object ClickLoadMoreCasts : CafeDashboardAction
    data object DismissInfoMessage : CafeDashboardAction
}
