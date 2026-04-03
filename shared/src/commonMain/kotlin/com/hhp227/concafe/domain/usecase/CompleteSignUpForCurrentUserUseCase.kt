package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository

class CompleteSignUpForCurrentUserUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        nickname: String,
        role: UserRole,
        affiliatedCafeId: String? = null,
        phoneNumber: String? = null
    ): AppResult<User> {
        return try {
            AppResult.Success(
                authRepository.completeSignUpForCurrentUser(
                    email = email,
                    nickname = nickname,
                    role = role,
                    affiliatedCafeId = affiliatedCafeId,
                    phoneNumber = phoneNumber
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
