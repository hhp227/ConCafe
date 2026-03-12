package com.hhp227.concafe.presentation.review

sealed interface ReviewEditEvent {
    data object NavigateBack : ReviewEditEvent
}
