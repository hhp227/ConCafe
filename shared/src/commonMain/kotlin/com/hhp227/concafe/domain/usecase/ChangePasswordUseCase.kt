package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository

class ChangePasswordUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String
    ): AppResult<Unit> {
        return try {
            authRepository.changePassword(
                currentPassword = currentPassword,
                newPassword = newPassword
            )
            AppResult.Success(Unit)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "change password failed"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
