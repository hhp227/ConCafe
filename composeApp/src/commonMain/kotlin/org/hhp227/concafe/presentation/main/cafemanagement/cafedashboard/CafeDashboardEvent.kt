package org.hhp227.concafe.presentation.main.cafemanagement.cafedashboard

sealed interface CafeDashboardEvent {
    data object NavigateBack : CafeDashboardEvent
}
