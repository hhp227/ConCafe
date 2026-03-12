package com.hhp227.concafe.presentation.main.cafemanagement.schedule

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val castSummary: CastSummary = CastSummary(),
    val weekRangeLabel: String = "",
    val weekDays: List<WeekDay> = emptyList(),
    val schedules: List<DaySchedule> = emptyList(),
    val selectedDayId: String = "",
    val infoMessage: String? = null
) {
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
        val isWorking: Boolean
    )
}
