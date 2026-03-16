package com.hhp227.concafe.presentation.main.cafemanagement.schedule

sealed interface ScheduleAction {
    data object ClickBack : ScheduleAction
    data object ClickMore : ScheduleAction
    data object ClickCalendar : ScheduleAction
    data class SelectDay(val dayId: String) : ScheduleAction
    data class ClickEditDay(val dayId: String) : ScheduleAction
    data object DismissEditSheet : ScheduleAction
    data class ChangeEditStatus(val status: ScheduleEditStatus) : ScheduleAction
    data class ChangeEditStartTime(val value: String) : ScheduleAction
    data class ChangeEditEndTime(val value: String) : ScheduleAction
    data object SubmitEditDay : ScheduleAction
    data object ClickSave : ScheduleAction
    data object DismissInfoMessage : ScheduleAction
}
