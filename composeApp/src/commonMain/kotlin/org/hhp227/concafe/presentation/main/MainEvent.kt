package com.hhp227.concafe.presentation.main

sealed interface MainEvent {
    data class ShowError(val message: String) : MainEvent
}
