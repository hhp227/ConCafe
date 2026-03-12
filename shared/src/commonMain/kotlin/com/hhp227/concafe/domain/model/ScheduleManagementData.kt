package com.hhp227.concafe.domain.model

data class ScheduleManagementData(
    val detail: CastDetail,
    val weekRangeLabel: String,
    val selectedDayId: String,
    val weekDays: List<ScheduleManagementWeekDay>,
    val daySchedules: List<ScheduleManagementDaySchedule>
)

data class ScheduleManagementWeekDay(
    val id: String,
    val label: String,
    val number: String,
    val isWorking: Boolean
)

data class ScheduleManagementDaySchedule(
    val id: String,
    val title: String,
    val timeLabel: String,
    val statusLabel: String,
    val isWorking: Boolean
)
