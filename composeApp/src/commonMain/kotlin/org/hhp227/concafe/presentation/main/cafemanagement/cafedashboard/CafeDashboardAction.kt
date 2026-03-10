package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardAction {
    data object ClickBack : CafeDashboardAction
    data class ClickShortcut(val shortcut: CafeDashboardUiState.Shortcut) : CafeDashboardAction
    data object DismissInfoMessage : CafeDashboardAction
}
