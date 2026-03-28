package com.hhp227.concafe.presentation.auth.resetpassword

sealed interface ResetPasswordEvent {
    data object NavigateBack : ResetPasswordEvent

    data class ShowMessage(val message: String) : ResetPasswordEvent
}
