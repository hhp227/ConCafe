package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.DeleteAccountRequest
import com.hhp227.concafe.domain.repository.AuthRepository

class DeleteAccountUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(request: DeleteAccountRequest): AppResult<Unit> {
        return try {
            AppResult.Success(authRepository.deleteAccount(request))
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "delete account failed"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
