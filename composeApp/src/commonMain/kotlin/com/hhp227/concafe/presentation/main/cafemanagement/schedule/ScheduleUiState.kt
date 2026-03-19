package com.hhp227.concafe.presentation.main.cafemanagement.schedule

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
            if (!isEditingWorking) return "0시간"
            val durationMinutes = computeDurationMinutes(editStartTime, editEndTime)
            val actualMinutes = (durationMinutes - 60).coerceAtLeast(0)
            val hours = actualMinutes / 60
            val minutes = actualMinutes % 60
            return if (minutes == 0) "${hours}시간" else "${hours}시간 ${minutes}분"
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
        private fun defaultTimeOptions(): List<String> {
            val options = mutableListOf<String>()
            for (hour in 8..23) {
                options += "%02d:00".format(hour)
                if (hour != 23) {
                    options += "%02d:30".format(hour)
                }
            }
            return options
        }

        private fun computeDurationMinutes(start: String, end: String): Int {
            fun parse(time: String): Int {
                val parts = time.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                return hour * 60 + minute
            }

            val startMinutes = parse(start)
            val endMinutes = parse(end)
            return (endMinutes - startMinutes).coerceAtLeast(0)
        }
    }
}
