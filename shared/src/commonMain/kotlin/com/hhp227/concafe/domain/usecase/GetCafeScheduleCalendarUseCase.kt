package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeCalendarDay
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class GetCafeScheduleCalendarUseCase(
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(
        cafeId: String,
        fromDate: String,
        toDate: String
    ): AppResult<List<CafeCalendarDay>> {
        return try {
            val from = LocalDate.parse(fromDate)
            val to = LocalDate.parse(toDate)
            val dates = buildList {
                var current = from
                while (current <= to) {
                    add(current.toString())
                    current = current.plus(DatePeriod(days = 1))
                }
            }
            val casts = castRepository.getCafeCasts(cafeId)
            val castNameById = casts.associate { it.id to it.name }
            val dayResults = coroutineScope {
                dates.map { date ->
                    async { date to castRepository.getWorkingCastIdsByCafeAndDate(cafeId, date) }
                }.map { it.await() }
            }
            AppResult.Success(
                dayResults.map { (date, castIds) ->
                    CafeCalendarDay(
                        date = date,
                        castNames = castIds.mapNotNull { castNameById[it] }.sorted()
                    )
                }
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
