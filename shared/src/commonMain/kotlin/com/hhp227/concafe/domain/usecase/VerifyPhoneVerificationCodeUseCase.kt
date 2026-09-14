package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository

class VerifyPhoneVerificationCodeUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): AppResult<Unit> {
        return try {
            AppResult.Success(authRepository.verifyPhoneVerificationCode(code.trim()))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid verification code"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
