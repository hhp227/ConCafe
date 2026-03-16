package com.hhp227.concafe.presentation.review

sealed interface ReviewEditAction {
    data object ClickBack : ReviewEditAction
    data class SelectRating(val rating: Int) : ReviewEditAction
    data object ClickAddPhoto : ReviewEditAction
    data class SelectPhoto(val imageUrl: String) : ReviewEditAction
    data object RemovePhoto : ReviewEditAction
    data class ChangeReviewText(val value: String) : ReviewEditAction
    data class ToggleCastTag(val castId: String) : ReviewEditAction
    data class SelectAtmosphereAnswer(val isPositive: Boolean) : ReviewEditAction
    data object ClickSubmit : ReviewEditAction
    data object DismissInfoMessage : ReviewEditAction
}
