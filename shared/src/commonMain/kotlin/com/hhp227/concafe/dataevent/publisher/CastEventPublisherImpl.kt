package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.CastEvent
import com.hhp227.concafe.domain.event.publisher.CastEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CastEventPublisherImpl : CastEventPublisher {
    private val _events = MutableSharedFlow<CastEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: CastEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<CastEvent> = _events.asSharedFlow()
}