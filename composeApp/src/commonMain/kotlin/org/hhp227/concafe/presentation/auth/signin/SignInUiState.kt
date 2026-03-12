package com.hhp227.concafe.presentation.auth.signin

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        fun empty() = SignInUiState()
    }
}
