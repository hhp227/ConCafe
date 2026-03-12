package org.hhp227.concafe.presentation.main.fanmanagement

sealed interface FanManagementEvent {
    data class ShowMessage(val message: String) : FanManagementEvent
    data class NavigateToCastEdit(val cafeId: String, val castId: String) : FanManagementEvent
}
