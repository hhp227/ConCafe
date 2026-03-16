package com.hhp227.concafe.presentation.settings.changepassword

sealed interface ChangePasswordAction {
    data object ClickBack : ChangePasswordAction
    data class ChangeCurrentPassword(val value: String) : ChangePasswordAction
    data class ChangeNewPassword(val value: String) : ChangePasswordAction
    data class ChangeConfirmPassword(val value: String) : ChangePasswordAction
    data object ClickSubmit : ChangePasswordAction
}
