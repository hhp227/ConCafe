package com.hhp227.concafe.presentation.auth.resetpassword

sealed interface ResetPasswordAction {
    data object ClickBack : ResetPasswordAction

    data class ChangeEmail(val value: String) : ResetPasswordAction

    data object ClickSubmit : ResetPasswordAction
}
