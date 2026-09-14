package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository

class RequestPhoneVerificationCodeUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String): AppResult<Unit> {
        return try {
            AppResult.Success(authRepository.requestPhoneVerificationCode(phoneNumber.trim()))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "phone verification failed"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
