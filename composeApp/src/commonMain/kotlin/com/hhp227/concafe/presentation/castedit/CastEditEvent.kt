package com.hhp227.concafe.presentation.castedit

sealed interface CastEditEvent {
    data object NavigateBack : CastEditEvent
}
