package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.repository.CafeRepository

class UpdateCafeTableCountsUseCase(
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(
        cafeId: String,
        current: Int,
        total: Int
    ): AppResult<Unit> {
        return try {
            cafeRepository.updateCafeTableCounts(cafeId, current, total)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
