package com.hhp227.concafe.presentation.main.cafemanagement.schedule

data class ScheduleUiState(
    val isSaving: Boolean = false,
    val castSummary: CastSummary = CastSummary(),
    val weekRangeLabel: String = "2024년 5월 12일 - 5월 18일",
    val weekDays: List<WeekDay> = defaultWeekDays(),
    val schedules: List<DaySchedule> = defaultDaySchedules(),
    val selectedDayId: String = defaultWeekDays().firstOrNull { it.isSelected }?.id.orEmpty(),
    val infoMessage: String? = null
) {
    data class CastSummary(
        val title: String = "사쿠라 (Sakura)",
        val subtitle: String = "메이드 / No. 12 / 에이스",
        val badge: String = "Cast Member"
    )

    data class WeekDay(
        val id: String,
        val label: String,
        val number: String,
        val isSelected: Boolean
    )

    data class DaySchedule(
        val id: String,
        val title: String,
        val timeLabel: String,
        val statusLabel: String,
        val isWorking: Boolean
    )
}

private fun defaultWeekDays(): List<ScheduleUiState.WeekDay> {
    return listOf(
        ScheduleUiState.WeekDay("2024-05-12", "12(일)", "12", false),
        ScheduleUiState.WeekDay("2024-05-13", "13(월)", "13", true),
        ScheduleUiState.WeekDay("2024-05-14", "14(화)", "14", false),
        ScheduleUiState.WeekDay("2024-05-15", "15(수)", "15", false),
        ScheduleUiState.WeekDay("2024-05-16", "16(목)", "16", false)
    )
}

private fun defaultDaySchedules(): List<ScheduleUiState.DaySchedule> {
    return listOf(
        ScheduleUiState.DaySchedule("2024-05-13", "5월 13일 (월)", "14:00 - 22:00 (8시간)", "근무 중", true),
        ScheduleUiState.DaySchedule("2024-05-14", "5월 14일 (화)", "16:00 - 23:00 (7시간)", "근무 중", true),
        ScheduleUiState.DaySchedule("2024-05-15", "5월 15일 (수)", "일정이 없습니다", "휴무", false),
        ScheduleUiState.DaySchedule("2024-05-16", "5월 16일 (목)", "14:00 - 22:00 (8시간)", "근무 중", true),
        ScheduleUiState.DaySchedule("2024-05-17", "5월 17일 (금)", "17:00 - 24:00 (7시간)", "근무 중", true)
    )
}
