package com.hhp227.concafe.presentation.cafe

sealed interface CafeEvent {
    data object NavigateBack : CafeEvent

    data class NavigateToCast(val id: String) : CafeEvent

    data class NavigateToCafeEvent(val cafeId: String, val eventId: String) : CafeEvent

    data class NavigateToReviewEdit(val cafeId: String, val reviewId: String? = null) : CafeEvent

    data class NavigateToPicture(val imageUrl: String) : CafeEvent

    data object NavigateToSignIn : CafeEvent

    data object ShowReviewDeleteFailedMessage : CafeEvent

    data object ShowReviewReportedMessage : CafeEvent
}
