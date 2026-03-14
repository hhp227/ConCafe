package com.hhp227.concafe.presentation.main.cafemanagement.schedule

data class ScheduleUiState(
    val managedCastId: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditSheetVisible: Boolean = false,
    val errorMessage: String? = null,
    val castSummary: CastSummary = CastSummary(),
    val weekRangeLabel: String = "",
    val weekDays: List<WeekDay> = emptyList(),
    val schedules: List<DaySchedule> = emptyList(),
    val selectedDayId: String = "",
    val infoMessage: String? = null,
    val editingScheduleId: String? = null,
    val editingScheduleTitle: String = "",
    val editStatus: ScheduleEditStatus = ScheduleEditStatus.WORK,
    val editStartTime: String = "10:00",
    val editEndTime: String = "19:00",
    val timeOptions: List<String> = defaultTimeOptions()
) {
    val isEditingWorking: Boolean
        get() = editStatus == ScheduleEditStatus.WORK

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

    data class WeekDay(
        val id: String,
        val label: String,
        val number: String,
        val isSelected: Boolean,
        val isWorking: Boolean
    )

    data class DaySchedule(
        val id: String,
        val title: String,
        val timeLabel: String,
        val statusLabel: String,
        val isWorking: Boolean,
        val status: ScheduleEditStatus = if (isWorking) ScheduleEditStatus.WORK else ScheduleEditStatus.OFF
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

enum class ScheduleEditStatus(val label: String) {
    WORK("근무"),
    OFF("휴무"),
    VACATION("휴가")
}
