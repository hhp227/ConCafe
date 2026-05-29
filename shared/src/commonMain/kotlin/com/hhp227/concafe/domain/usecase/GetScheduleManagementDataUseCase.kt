package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.ScheduleManagementData
import com.hhp227.concafe.domain.model.ScheduleManagementDaySchedule
import com.hhp227.concafe.domain.model.ScheduleManagementWeekDay
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class GetScheduleManagementDataUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(castId: String? = null): AppResult<ScheduleManagementData> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            val resolvedCastId = castId ?: run {
                if (currentUser.role != UserRole.CAST) {
                    return AppResult.Failure(AppError.PermissionDenied)
                }
                castRepository.getCastByLinkedUserId(currentUser.id)
                    ?.id
                    ?: return AppResult.Failure(AppError.NotFound)
            }
            val nowDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val today = nowDateTime.date
            val weekStart = today.toWeekStart()
            val scheduleEnd = today.plus(DatePeriod(months = 1)).toMonthEnd()
            val loaded = coroutineScope {
                val detailDeferred = async {
                    castRepository.getCastDetail(resolvedCastId)
                }
                val schedulesDeferred = async {
                    castRepository.getCastSchedules(
                        castId = resolvedCastId,
                        fromDate = weekStart.toString(),
                        toDate = scheduleEnd.toString()
                    )
                }
                val scheduleStatusesDeferred = async {
                    castRepository.getCastScheduleStatuses(
                        castId = resolvedCastId,
                        fromDate = weekStart.toString(),
                        toDate = scheduleEnd.toString()
                    )
                }
                ScheduleLoadResult(
                    detail = detailDeferred.await(),
                    scheduleByDate = schedulesDeferred.await().associateBy { schedule -> schedule.date },
                    scheduleStatusByDate = scheduleStatusesDeferred.await()
                )
            }
            val detail = loaded.detail
            val scheduleByDate = loaded.scheduleByDate
            val scheduleStatusByDate = loaded.scheduleStatusByDate
            val scheduleDates = weekStart.datesUntil(scheduleEnd)
            val weekEnd = weekStart.plus(DatePeriod(days = 6))

            AppResult.Success(
                ScheduleManagementData(
                    detail = detail,
                    weekRangeLabel = "${weekStart.year}년 ${weekStart.monthNumber}월 ${weekStart.dayOfMonth}일 - ${weekEnd.monthNumber}월 ${weekEnd.dayOfMonth}일",
                    selectedDayId = today.toString(),
                    weekDays = scheduleDates.map { date ->
                        val status = scheduleStatusByDate[date.toString()]
                            ?: if (scheduleByDate.containsKey(date.toString())) CastScheduleStatus.WORK else CastScheduleStatus.OFF
                        ScheduleManagementWeekDay(
                            id = date.toString(),
                            label = "${date.dayOfMonth}(${date.toKoreanDayLabel()})",
                            number = date.dayOfMonth.toString(),
                            isWorking = status == CastScheduleStatus.WORK
                        )
                    },
                    daySchedules = scheduleDates.map { date ->
                        val schedule = scheduleByDate[date.toString()]
                        val status = scheduleStatusByDate[date.toString()]
                            ?: if (schedule != null) CastScheduleStatus.WORK else CastScheduleStatus.OFF
                        ScheduleManagementDaySchedule(
                            id = date.toString(),
                            title = "${date.monthNumber}월 ${date.dayOfMonth}일 (${date.toKoreanDayLabel()})",
                            timeLabel = if (status == CastScheduleStatus.WORK && schedule != null) {
                                "${schedule.startTime} - ${schedule.endTime} (${calculateHourLabel(schedule.startTime, schedule.endTime)})"
                            } else {
                                "일정이 없습니다"
                            },
                            statusLabel = resolveStatusLabel(
                                date = date,
                                today = today,
                                status = status,
                                startTime = schedule?.startTime,
                                endTime = schedule?.endTime,
                                nowDateTime = nowDateTime
                            ),
                            isWorking = status == CastScheduleStatus.WORK,
                            status = status
                        )
                    }
                )
            )
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}

private data class ScheduleLoadResult(
    val detail: com.hhp227.concafe.domain.model.CastDetail,
    val scheduleByDate: Map<String, com.hhp227.concafe.domain.model.CastSchedule>,
    val scheduleStatusByDate: Map<String, CastScheduleStatus>
)

private fun resolveStatusLabel(
    date: LocalDate,
    today: LocalDate,
    status: CastScheduleStatus,
    startTime: String?,
    endTime: String?,
    nowDateTime: LocalDateTime
): String {
    if (status != CastScheduleStatus.WORK) {
        return when (status) {
            CastScheduleStatus.OFF -> "휴무"
            CastScheduleStatus.VACATION -> "휴가"
            else -> "휴무"
        }
    }
    if (date != today || startTime == null || endTime == null) {
        return "근무"
    }
    val currentTotal = nowDateTime.hour * 60 + nowDateTime.minute
    val startTotal = (startTime.substringBefore(':').toIntOrNull() ?: 0) * 60 +
        (startTime.substringAfter(':').toIntOrNull() ?: 0)
    val endTotal = (endTime.substringBefore(':').toIntOrNull() ?: 0) * 60 +
        (endTime.substringAfter(':').toIntOrNull() ?: 0)
    return when {
        currentTotal < startTotal -> "출근 예정"
        currentTotal < endTotal -> "출근 중"
        else -> "근무 완료"
    }
}

private fun LocalDate.toWeekStart(): LocalDate {
    val daysFromSunday = dayOfWeek.isoDayNumber % 7
    return minus(DatePeriod(days = daysFromSunday))
}

private fun LocalDate.toMonthEnd(): LocalDate {
    val nextMonthStart = LocalDate(year, monthNumber, 1).plus(DatePeriod(months = 1))
    return nextMonthStart.minus(DatePeriod(days = 1))
}

private fun LocalDate.datesUntil(endInclusive: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = this
    while (current <= endInclusive) {
        dates += current
        current = current.plus(DatePeriod(days = 1))
    }
    return dates
}

private fun LocalDate.toKoreanDayLabel(): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
        else -> ""
    }
}

private fun calculateHourLabel(startTime: String, endTime: String): String {
    val startHour = startTime.substringBefore(':').toIntOrNull() ?: return "0시간"
    val endHour = endTime.substringBefore(':').toIntOrNull() ?: return "0시간"
    return "${(endHour - startHour).coerceAtLeast(0)}시간"
}
