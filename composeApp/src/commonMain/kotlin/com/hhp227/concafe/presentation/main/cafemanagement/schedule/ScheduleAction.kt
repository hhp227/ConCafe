package com.hhp227.concafe.presentation.main.cafemanagement.schedule

import com.hhp227.concafe.domain.model.CastScheduleStatus

sealed interface ScheduleAction {
    data object ClickBack : ScheduleAction
    data object ClickMore : ScheduleAction
    data object ClickCalendar : ScheduleAction
    data class SelectPeriod(val period: SchedulePeriod) : ScheduleAction
    data class SelectDay(val dayId: String) : ScheduleAction
    data class ClickEditDay(val dayId: String) : ScheduleAction
    data object DismissEditSheet : ScheduleAction
    data class ChangeEditStatus(val status: CastScheduleStatus) : ScheduleAction
    data class ChangeEditStartTime(val value: String) : ScheduleAction
    data class ChangeEditEndTime(val value: String) : ScheduleAction
    data object SubmitEditDay : ScheduleAction
    data object ClickSave : ScheduleAction
    data object DismissInfoMessage : ScheduleAction
}
