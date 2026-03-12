package com.hhp227.concafe.presentation.cafe

sealed interface CafeEvent {
    data object NavigateBack : CafeEvent

    data class NavigateToCast(val id: String) : CafeEvent

    data object NavigateToSignIn : CafeEvent
}
