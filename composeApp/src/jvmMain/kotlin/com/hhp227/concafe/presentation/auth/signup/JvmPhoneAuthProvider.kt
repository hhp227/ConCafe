package com.hhp227.concafe.presentation.auth.signup

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult

class JvmPhoneAuthProvider : PhoneAuthProvider {
    override suspend fun sendCode(phoneNumber: String): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown("phone verification is not supported on desktop"))
    }

    override suspend fun verifyCode(code: String): AppResult<Unit> {
        return AppResult.Failure(AppError.Unknown("phone verification is not supported on desktop"))
    }
}
