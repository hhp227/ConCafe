package com.hhp227.concafe.presentation.settings.changepassword

data class ChangePasswordUiState(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
    val isSubmitting: Boolean
) {
    companion object {
        fun empty(): ChangePasswordUiState = ChangePasswordUiState("", "", "", false)
    }
}
