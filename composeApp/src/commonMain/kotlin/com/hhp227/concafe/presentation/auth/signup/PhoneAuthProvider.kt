package com.hhp227.concafe.presentation.auth.signup

import com.hhp227.concafe.domain.common.AppResult

interface PhoneAuthProvider {
    suspend fun sendCode(phoneNumber: String): AppResult<Unit>
    suspend fun verifyCode(code: String): AppResult<Unit>
    suspend fun linkEmail(email: String, password: String): AppResult<Unit>
    suspend fun cleanupIncompleteAccount(): AppResult<Unit>
}
