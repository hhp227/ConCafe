package com.hhp227.concafe.presentation.main.cafemanagement.castlist

sealed interface CastListEvent {
    data object NavigateBack : CastListEvent
    data class NavigateToCastEdit(val cafeId: String, val castId: String? = null) : CastListEvent
    data class NavigateToSchedule(val castId: String) : CastListEvent
}
