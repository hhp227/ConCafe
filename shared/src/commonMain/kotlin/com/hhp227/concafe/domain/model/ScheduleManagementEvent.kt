package com.hhp227.concafe.domain.model

sealed class ScheduleManagementEvent {
    data class Updated(
        val castId: String,
        val date: String,
        val status: CastScheduleStatus
    ) : ScheduleManagementEvent()
}
