package com.hhp227.concafe.presentation.settings.account

sealed interface AccountSettingsAction {
    data object ClickBack : AccountSettingsAction
    data class ChangeNickname(val value: String) : AccountSettingsAction
    data object ClickSaveUserInfo : AccountSettingsAction
    data object ClickOpenCastEdit : AccountSettingsAction
    data object ClickOpenChangePassword : AccountSettingsAction
    data object ClickShowDeleteDialog : AccountSettingsAction
    data object ClickDismissDeleteDialog : AccountSettingsAction
    data class ChangeDeleteConfirmation(val value: String) : AccountSettingsAction
    data object ClickDeleteAccount : AccountSettingsAction
}
