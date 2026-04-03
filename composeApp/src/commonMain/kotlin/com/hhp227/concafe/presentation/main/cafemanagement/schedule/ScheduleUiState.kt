package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.domain.model.CastScheduleStatus
import com.hhp227.concafe.domain.model.ScheduleManagementDaySchedule
import com.hhp227.concafe.domain.model.ScheduleManagementWeekDay

data class ScheduleUiState(
    val managedCastId: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditSheetVisible: Boolean = false,
    val errorMessage: String? = null,
    val castSummary: CastSummary = CastSummary(),
    val weekRangeLabel: String = "",
    val weekDays: List<ScheduleManagementWeekDay> = emptyList(),
    val schedules: List<ScheduleManagementDaySchedule> = emptyList(),
    val selectedDayId: String = "",
    val infoMessage: String? = null,
    val editingScheduleId: String? = null,
    val editingScheduleTitle: String = "",
    val editStatus: CastScheduleStatus = CastScheduleStatus.WORK,
    val editStartTime: String = "10:00",
    val editEndTime: String = "19:00",
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

    data class CastSummary(
        val title: String = "",
        val subtitle: String = "",
        val badge: String = "Cast Member",
        val initials: String = ""
    )

    data class PendingScheduleUpdate(
        val date: String,
        val status: CastScheduleStatus,
        val startTime: String?,
        val endTime: String?
    )

    companion object {
        private fun defaultTimeOptions(): List<String> = TimeUtils.defaultHalfHourTimeOptions()
    }
}
