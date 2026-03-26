package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRepository

class ToggleFavoriteCafeUseCase(
    private val authRepository: AuthRepository,
    private val cafeRepository: CafeRepository,
    private val cafeDetailEventPublisher: CafeDetailEventPublisher
) {
    suspend operator fun invoke(cafeId: String): AppResult<Boolean> {
        return try {
            val currentUser = authRepository.getCurrentUser()

            if (currentUser != null) {
                val isFavorite = cafeRepository.toggleFavorite(currentUser.id, cafeId)
                cafeDetailEventPublisher.publish(
                    CafeDetailEvent.FavoriteToggled(
                        cafeId = cafeId,
                        isFavorite = isFavorite
                    )
                )
                AppResult.Success(isFavorite)
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
