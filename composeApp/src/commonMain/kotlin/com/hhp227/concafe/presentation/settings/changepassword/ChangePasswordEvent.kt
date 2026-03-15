package com.hhp227.concafe.presentation.settings.changepassword

sealed interface ChangePasswordEvent {
    data object NavigateBack : ChangePasswordEvent
    data class ShowMessage(val message: String) : ChangePasswordEvent
}
