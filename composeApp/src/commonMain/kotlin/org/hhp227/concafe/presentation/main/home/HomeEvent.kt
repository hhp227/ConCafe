package org.hhp227.concafe.presentation.main.home

sealed interface HomeEvent {
    data class NavigateToCastDetail(val id: String) : HomeEvent
    data class NavigateToCafeDetail(val id: String) : HomeEvent
}