package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.Cast
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class GetBirthdayCastsUseCase(
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(limit: Int = BIRTHDAY_CAST_LIMIT): AppResult<List<Cast>> {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            AppResult.Success(
                castRepository.getBirthdayCasts(
                    month = today.monthNumber,
                    dayOfMonth = today.dayOfMonth,
                    limit = limit.coerceAtLeast(1)
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
