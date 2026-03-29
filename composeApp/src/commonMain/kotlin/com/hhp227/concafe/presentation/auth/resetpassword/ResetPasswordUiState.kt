package com.hhp227.concafe.presentation.auth.resetpassword

data class ResetPasswordUiState(
    val email: String,
    val isSubmitting: Boolean
) {
    companion object {
        fun empty(): ResetPasswordUiState {
            return ResetPasswordUiState(
                email = "",
                isSubmitting = false
            )
        }
    }
}
