package com.hhp227.concafe.presentation.external

sealed interface ExternalLinkAction {
    data object ClickBack : ExternalLinkAction
}
