package com.hhp227.concafe.presentation.main.cafemanagement

sealed interface CafeManagementAction {
    data class ClickCafe(val cafeId: String) : CafeManagementAction
    data class ClickCafeDetail(val cafeId: String) : CafeManagementAction
    data class ChangeCafeSearchQuery(val query: String) : CafeManagementAction
    data class ClickClaimCafe(val cafeId: String) : CafeManagementAction
    data object ToggleCafeListExpanded : CafeManagementAction
    data object ClickCreateCafe : CafeManagementAction
    data object DismissInfoMessage : CafeManagementAction
}
