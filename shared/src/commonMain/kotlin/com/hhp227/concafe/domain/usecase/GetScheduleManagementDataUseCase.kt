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
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
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
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val weekStart = today.toWeekStart()
            val weekEnd = weekStart.plus(DatePeriod(days = 6))
            val detail = castRepository.getCastDetail(resolvedCastId)
            val scheduleByDate = castRepository.getCastSchedules(
                castId = resolvedCastId,
                fromDate = weekStart.toString(),
                toDate = weekEnd.toString()
            ).associateBy { it.date }
            val scheduleStatusByDate = castRepository.getCastScheduleStatuses(
                castId = resolvedCastId,
                fromDate = weekStart.toString(),
                toDate = weekEnd.toString()
            )
            val weekDates = (0..6).map { weekStart.plus(DatePeriod(days = it)) }

            AppResult.Success(
                ScheduleManagementData(
                    detail = detail,
                    weekRangeLabel = "${weekStart.year}년 ${weekStart.monthNumber}월 ${weekStart.dayOfMonth}일 - ${weekEnd.monthNumber}월 ${weekEnd.dayOfMonth}일",
                    selectedDayId = today.toString(),
                    weekDays = weekDates.map { date ->
                        val status = scheduleStatusByDate[date.toString()]
                            ?: if (scheduleByDate.containsKey(date.toString())) CastScheduleStatus.WORK else CastScheduleStatus.OFF
                        ScheduleManagementWeekDay(
                            id = date.toString(),
                            label = "${date.dayOfMonth}(${date.toKoreanDayLabel()})",
                            number = date.dayOfMonth.toString(),
                            isWorking = status == CastScheduleStatus.WORK
                        )
                    },
                    daySchedules = weekDates.map { date ->
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
                            statusLabel = when (status) {
                                CastScheduleStatus.WORK -> "근무 중"
                                CastScheduleStatus.OFF -> "휴무"
                                CastScheduleStatus.VACATION -> "휴가"
                            },
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

private fun LocalDate.toWeekStart(): LocalDate {
    val daysFromSunday = dayOfWeek.isoDayNumber % 7
    return minus(DatePeriod(days = daysFromSunday))
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
