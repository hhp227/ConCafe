package com.hhp227.concafe.presentation.settings.account

import com.hhp227.concafe.domain.model.MyInfoFeed
import com.hhp227.concafe.domain.model.UserRole

data class AccountSettingsUiState(
    val isLoading: Boolean,
    val errorMessage: String?,
    val myInfoFeed: MyInfoFeed?,
    val nicknameInput: String,
    val emailInput: String,
    val isDeleteDialogVisible: Boolean,
    val deleteConfirmation: String,
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
                emailInput = "",
                isDeleteDialogVisible = false,
                deleteConfirmation = "",
                isDeleteRequested = false
            )
        }
    }
}
