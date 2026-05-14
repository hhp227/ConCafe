package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.ScheduleManagementDaySchedule
import com.hhp227.concafe.domain.model.ScheduleManagementWeekDay

data class ScheduleUiState(
    val managedCastId: String = "",
    val managedCafeId: String = "",
    val managedCafeName: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditSheetVisible: Boolean = false,
    val errorMessage: String? = null,
    val castSummary: CastSummary = CastSummary(),
    val schedulePeriod: SchedulePeriod = SchedulePeriod.ONE_WEEK,
    val weekRangeLabel: String = "",
    val weekDays: List<ScheduleManagementWeekDay> = emptyList(),
    val schedules: List<ScheduleManagementDaySchedule> = emptyList(),
    val allWeekDays: List<ScheduleManagementWeekDay> = emptyList(),
    val allSchedules: List<ScheduleManagementDaySchedule> = emptyList(),
    val selectedDayId: String = "",
    val infoMessage: String? = null,
    val editingScheduleId: String? = null,
    val editingScheduleTitle: String = "",
    val editStatus: CastScheduleStatus = CastScheduleStatus.WORK,
    val editStartTime: String = DEFAULT_START_TIME,
    val editEndTime: String = DEFAULT_END_TIME,
    val pendingUpdates: List<PendingScheduleUpdate> = emptyList(),
    val timeOptions: List<String> = defaultTimeOptions()
) {
    val hasPendingChanges: Boolean
        get() = pendingUpdates.isNotEmpty()

    val isEditingWorking: Boolean
        get() = editStatus == CastScheduleStatus.WORK

    val totalWorkDurationLabel: String
        get() {
            if (!isEditingWorking) return "schedule_duration_hours_only:0"
            val durationMinutes = TimeUtils.computeDurationMinutes(editStartTime, editEndTime)
            val actualMinutes = (durationMinutes - 60).coerceAtLeast(0)
            val hours = actualMinutes / 60
            val minutes = actualMinutes % 60
            return if (minutes == 0) {
                "schedule_duration_hours_only:$hours"
            } else {
                "schedule_duration_hours_minutes:$hours:$minutes"
            }
        }

    fun withSchedulePeriod(period: SchedulePeriod): ScheduleUiState {
        val visibleWeekDays = allWeekDays.take(period.dayCount)
        val visibleSchedules = allSchedules.take(period.dayCount)
        return copy(
            schedulePeriod = period,
            weekRangeLabel = resolveRangeLabel(visibleWeekDays),
            weekDays = visibleWeekDays,
            schedules = visibleSchedules,
            selectedDayId = selectedDayId.takeIf { selected -> visibleWeekDays.any { it.id == selected } }
                ?: visibleWeekDays.firstOrNull()?.id.orEmpty()
        )
    }

    data class CastSummary(
        val title: String = "",
        val subtitle: String = "",
        val badge: String = "Cast Member",
        val initials: String = "",
        val profileImageUrl: String? = null
    )

    data class PendingScheduleUpdate(
        val date: String,
        val status: CastScheduleStatus,
        val startTime: String?,
        val endTime: String?
    )

    companion object {
        const val DEFAULT_START_TIME = "14:00"
        const val DEFAULT_END_TIME = "22:00"

        private fun defaultTimeOptions(): List<String> = TimeUtils.defaultHalfHourTimeOptions()
    }
}

enum class SchedulePeriod(val dayCount: Int) {
    ONE_WEEK(7),
    TWO_WEEKS(14),
    ONE_MONTH(30)
}

private fun resolveRangeLabel(days: List<ScheduleManagementWeekDay>): String {
    val first = days.firstOrNull()?.id ?: return ""
    val last = days.lastOrNull()?.id ?: return ""
    return "${first.substring(0, 4)}년 ${first.substring(5, 7).toIntOrNull() ?: 0}월 ${first.substring(8, 10).toIntOrNull() ?: 0}일 - ${last.substring(5, 7).toIntOrNull() ?: 0}월 ${last.substring(8, 10).toIntOrNull() ?: 0}일"
}
