package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult

class VerifyPhoneVerificationCodeUseCase {
    operator fun invoke(code: String): AppResult<Unit> {
        val normalized = code.trim()
        return if (normalized.length < MIN_VERIFICATION_CODE_LENGTH) {
            AppResult.Failure(AppError.ValidationFailed("invalid verification code"))
        } else {
            AppResult.Success(Unit)
        }
    }

    private companion object {
        const val MIN_VERIFICATION_CODE_LENGTH = 4
    }
}
