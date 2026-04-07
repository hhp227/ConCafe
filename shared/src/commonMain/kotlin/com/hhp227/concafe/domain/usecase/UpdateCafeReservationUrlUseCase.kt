package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.CafeRepository

class UpdateCafeReservationUrlUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(cafeId: String, reservationUrl: String?): AppResult<Unit> {
        return try {
            cafeRepository.updateCafeReservationUrl(
                cafeId = cafeId,
                reservationUrl = reservationUrl?.trim()?.takeIf { it.isNotEmpty() }
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
