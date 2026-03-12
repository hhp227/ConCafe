package com.hhp227.concafe.presentation.main.cafemanagement.schedule

sealed interface ScheduleEvent {
    data object NavigateBack : ScheduleEvent
    data class ShowMessage(val message: String) : ScheduleEvent
}
