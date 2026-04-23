package com.hhp227.concafe.presentation.cafe.event

sealed interface CafeEventEvent {
    data object NavigateBack : CafeEventEvent
}
