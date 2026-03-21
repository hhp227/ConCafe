package com.hhp227.concafe.presentation.settings.account

import com.hhp227.concafe.domain.model.MyInfoFeed
import com.hhp227.concafe.domain.model.UserRole

data class AccountSettingsUiState(
    val isLoading: Boolean,
    val errorMessage: String?,
    val myInfoFeed: MyInfoFeed?,
    val nicknameInput: String,
    val isDeleteDialogVisible: Boolean,
    val deletePassword: String,
    val deletePasswordErrorMessage: String?,
    val isDeleteRequested: Boolean
) {
    val role: UserRole?
        get() = myInfoFeed?.user?.role

    companion object {
        fun empty(): AccountSettingsUiState {
            return AccountSettingsUiState(
                isLoading = true,
                errorMessage = null,
                myInfoFeed = null,
                nicknameInput = "",
                isDeleteDialogVisible = false,
                deletePassword = "",
                deletePasswordErrorMessage = null,
                isDeleteRequested = false
            )
        }
    }
}
