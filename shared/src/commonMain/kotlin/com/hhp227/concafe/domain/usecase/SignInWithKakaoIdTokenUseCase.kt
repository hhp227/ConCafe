package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.User
import com.hhp227.concafe.domain.repository.AuthRepository

class SignInWithKakaoIdTokenUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): AppResult<User> {
        return try {
            AppResult.Success(authRepository.signInWithKakaoIdToken(idToken))
        } catch (e: IllegalArgumentException) {
            println("TEST, SignInWithKakaoIdTokenUseCase validation error: ${e.message}")
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            println("TEST, SignInWithKakaoIdTokenUseCase unknown error: ${e.message}")
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
