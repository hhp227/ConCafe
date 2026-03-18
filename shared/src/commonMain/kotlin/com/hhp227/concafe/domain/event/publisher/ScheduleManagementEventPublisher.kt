package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.ScheduleManagementEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface ScheduleManagementEventPublisher {
    fun publish(event: ScheduleManagementEvent)

    @NativeCoroutines
    fun observe(): Flow<ScheduleManagementEvent>
}