package com.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardAction {
    data object ClickBack : CafeDashboardAction
    data class ClickShortcut(val shortcut: CafeDashboardShortcut) : CafeDashboardAction
    data object DismissExternalLinkSheet : CafeDashboardAction
    data class ChangeExternalLinkTitle(val value: String) : CafeDashboardAction
    data class ChangeExternalLinkUrl(val value: String) : CafeDashboardAction
    data object SubmitExternalLink : CafeDashboardAction
    data class ClickExternalLinkItem(val linkId: String) : CafeDashboardAction
    data class ClickDeleteExternalLink(val linkId: String) : CafeDashboardAction
    data class ClickCastSchedule(val castId: String) : CafeDashboardAction
    data object ClickDeleteCast : CafeDashboardAction
    data object ConfirmDeleteCast : CafeDashboardAction
    data object DismissDeleteCastDialog : CafeDashboardAction
    data class ClickApproveCastClaim(val claimId: String) : CafeDashboardAction
    data class ClickRejectCastClaim(val claimId: String) : CafeDashboardAction
    data object ClickLoadMoreCasts : CafeDashboardAction
    data object DismissInfoMessage : CafeDashboardAction
}
