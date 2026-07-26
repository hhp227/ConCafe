package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.CastScheduleStatus

sealed class ScheduleManagementEvent {
    data class Updated(
        val castId: String,
        val date: String,
        val status: CastScheduleStatus,
        val startTime: String? = null,
        val endTime: String? = null
    ) : ScheduleManagementEvent()
}