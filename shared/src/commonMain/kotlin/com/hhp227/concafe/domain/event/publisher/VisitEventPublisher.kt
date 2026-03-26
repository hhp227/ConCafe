package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.VisitEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class VisitEventPublisher {
    private val _events = MutableSharedFlow<VisitEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events: SharedFlow<VisitEvent> = _events.asSharedFlow()

    fun publish(event: VisitEvent) {
        _events.tryEmit(event)
    }
}
