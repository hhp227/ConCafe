package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.ScheduleManagementEvent
import com.hhp227.concafe.domain.event.publisher.ScheduleManagementEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ScheduleManagementEventPublisherImpl : ScheduleManagementEventPublisher {
    private val _events = MutableSharedFlow<ScheduleManagementEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: ScheduleManagementEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<ScheduleManagementEvent> {
        return _events.asSharedFlow()
    }
}