package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.ScheduleManagementEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ScheduleManagementEventPublisher {
    private val _events = MutableSharedFlow<ScheduleManagementEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: ScheduleManagementEvent) {
        _events.tryEmit(event)
    }
}