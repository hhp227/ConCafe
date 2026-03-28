package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.AuthRepository

class SignInWithKakaoIdTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        idToken: String,
        email: String? = null,
        nickname: String? = null
    ): AppResult<User> {
        return try {
            AppResult.Success(
                authRepository.signInWithKakaoIdToken(
                    idToken = idToken,
                    email = email,
                    nickname = nickname
                )
            )
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
