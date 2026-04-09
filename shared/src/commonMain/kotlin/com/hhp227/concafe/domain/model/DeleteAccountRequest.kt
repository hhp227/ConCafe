package com.hhp227.concafe.domain.model

data class DeleteAccountRequest(
    val provider: AuthProvider,
    val password: String? = null,
    val idToken: String? = null
)
