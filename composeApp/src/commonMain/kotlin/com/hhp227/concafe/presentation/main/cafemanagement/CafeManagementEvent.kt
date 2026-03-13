package com.hhp227.concafe.presentation.main.cafemanagement

sealed interface CafeManagementEvent {
    data class NavigateToCafeDashboard(val cafeId: String) : CafeManagementEvent
    data class NavigateToCafe(val cafeId: String) : CafeManagementEvent
    data object NavigateToCafeInfoRegistration : CafeManagementEvent
}
