package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository

class LinkPhoneCredentialUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String): AppResult<Unit> {
        return try {
            AppResult.Success(authRepository.linkPhoneCredential(code.trim()))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "phone link failed"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
