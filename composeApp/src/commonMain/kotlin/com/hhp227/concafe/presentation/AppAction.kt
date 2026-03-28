package com.hhp227.concafe.presentation

sealed interface AppAction {
    data class SyncPushToken(val token: String) : AppAction
}
