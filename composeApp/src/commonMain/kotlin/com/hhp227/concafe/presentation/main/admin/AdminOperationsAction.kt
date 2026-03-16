package com.hhp227.concafe.presentation.main.admin

sealed interface AdminOperationsAction {
    data object ClickNotifications : AdminOperationsAction
    data object ClickSeeAllPending : AdminOperationsAction
    data object ClickBannerRegister : AdminOperationsAction
    data class SelectPendingFilter(val filter: PendingFilter) : AdminOperationsAction
    data class ApprovePending(val id: String) : AdminOperationsAction
    data class RejectPending(val id: String) : AdminOperationsAction
    data class ClickQuickMenu(val id: String) : AdminOperationsAction
    data object DismissInfoMessage : AdminOperationsAction
}
