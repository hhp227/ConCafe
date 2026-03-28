package com.hhp227.concafe.presentation

sealed interface AppEvent {
    data object SyncPushToken : AppEvent
}
