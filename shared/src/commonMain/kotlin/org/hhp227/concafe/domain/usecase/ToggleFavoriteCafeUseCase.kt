package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRepository

class ToggleFavoriteCafeUseCase(
    private val authRepository: AuthRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(cafeId: String): AppResult<Boolean> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser != null) {
                AppResult.Success(cafeRepository.toggleFavorite(currentUser.id, cafeId))
            } else {
                AppResult.Failure(AppError.Unauthorized)
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
