package com.hhp227.concafe.presentation.external

sealed interface ExternalLinkEvent {
    data object NavigateBack : ExternalLinkEvent
}
