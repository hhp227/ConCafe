package com.hhp227.concafe.presentation.main.checkin.map

sealed interface MapEvent {
    data object NavigateBack : MapEvent

    data class NavigateToCafe(val id: String) : MapEvent
}
