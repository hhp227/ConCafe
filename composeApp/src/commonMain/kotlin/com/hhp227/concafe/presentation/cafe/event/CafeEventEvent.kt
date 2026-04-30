package com.hhp227.concafe.presentation.cafe.event

sealed interface CafeEventEvent {
    data object NavigateBack : CafeEventEvent
    data class NavigateToCast(val castId: String) : CafeEventEvent
    data object NavigateToSignIn : CafeEventEvent
    data object NavigateToCafe : CafeEventEvent
}
