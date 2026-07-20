package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomeBirthdayCastPage
import com.hhp227.concafe.domain.repository.CafeRepository
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetBirthdayCastsUseCase(
    private val castRepository: CastRepository,
    private val cafeRepository: CafeRepository
) {
    suspend operator fun invoke(limit: Int = BIRTHDAY_CAST_LIMIT): AppResult<HomeBirthdayCastPage> {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val casts = castRepository.getBirthdayCasts(
                month = today.monthNumber,
                dayOfMonth = today.dayOfMonth,
                limit = limit.coerceAtLeast(1)
            )
            val cafeIds = casts.map { it.cafeId }.distinct()
            val cafes = if (cafeIds.isEmpty()) {
                emptyList()
            } else {
                runCatching { cafeRepository.getCafesByIds(cafeIds) }.getOrElse { emptyList() }
            }
            AppResult.Success(
                HomeBirthdayCastPage(
                    casts = casts,
                    cafeNames = cafes.associate { it.id to it.name }
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        private const val BIRTHDAY_CAST_LIMIT = 6
    }
}
