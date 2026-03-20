package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.AuthRepository

class RestoreSessionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<User?> {
        return try {
            AppResult.Success(authRepository.restoreSession())
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "session restore failed"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
