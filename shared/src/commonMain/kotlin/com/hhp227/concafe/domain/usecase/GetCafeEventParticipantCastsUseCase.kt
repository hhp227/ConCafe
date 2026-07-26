package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.repository.CastRepository

class GetCafeEventParticipantCastsUseCase(
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(castIds: List<String>): AppResult<List<Cast>> {
        return try {
            val normalizedIds = castIds
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            if (normalizedIds.isEmpty()) {
                AppResult.Success(emptyList())
            } else {
                AppResult.Success(castRepository.getCastsByIds(normalizedIds))
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
