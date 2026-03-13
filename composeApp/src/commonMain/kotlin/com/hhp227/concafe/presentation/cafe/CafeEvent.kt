package com.hhp227.concafe.presentation.cafe

sealed interface CafeEvent {
    data object NavigateBack : CafeEvent

    data class NavigateToCast(val id: String) : CafeEvent

    data class NavigateToReviewEdit(val cafeId: String) : CafeEvent

    data object NavigateToSignIn : CafeEvent

    data object ScrollReviewsToTop : CafeEvent
}
