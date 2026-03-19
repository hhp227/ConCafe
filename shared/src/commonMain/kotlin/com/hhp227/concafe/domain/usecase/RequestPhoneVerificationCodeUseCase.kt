package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult

class RequestPhoneVerificationCodeUseCase {
    operator fun invoke(phone: String): AppResult<String> {
        val normalizedPhone = phone.trim()
        return if (normalizedPhone.isBlank()) {
            AppResult.Failure(AppError.ValidationFailed("phone is required"))
        } else {
            AppResult.Success("인증번호가 $normalizedPhone 로 전송되었습니다.")
        }
    }
}
