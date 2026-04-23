package com.hhp227.concafe.presentation.cafe.event

sealed interface CafeEventAction {
    data object ClickBack : CafeEventAction

    data object Retry : CafeEventAction
}
