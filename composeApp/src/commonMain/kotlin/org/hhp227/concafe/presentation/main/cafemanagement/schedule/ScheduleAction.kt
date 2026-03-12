package org.hhp227.concafe.presentation.main.cafemanagement.schedule

sealed interface ScheduleAction {
    data object ClickBack : ScheduleAction
    data object ClickMore : ScheduleAction
    data object ClickCalendar : ScheduleAction
    data class SelectDay(val dayId: String) : ScheduleAction
    data class ClickEditDay(val dayId: String) : ScheduleAction
    data object ClickSave : ScheduleAction
    data object DismissInfoMessage : ScheduleAction
}
