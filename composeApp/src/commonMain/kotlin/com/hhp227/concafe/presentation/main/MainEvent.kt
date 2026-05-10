package com.hhp227.concafe.presentation.main

import com.hhp227.concafe.domain.model.AppUpdateInfo

sealed interface MainEvent {
    data class ShowError(val message: String) : MainEvent
    data class ShowAppUpdate(val updateInfo: AppUpdateInfo) : MainEvent
    data object NavigateToSignUp : MainEvent
}
