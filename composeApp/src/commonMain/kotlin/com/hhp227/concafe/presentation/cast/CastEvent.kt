package com.hhp227.concafe.presentation.cast

sealed interface CastEvent {
    data object NavigateBack : CastEvent

    data class NavigateToCafe(val id: String) : CastEvent

    data object NavigateToSignIn : CastEvent

    data class NavigateToPicture(val imageUrl: String) : CastEvent
}
