package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.CastClaimEvent
import com.hhp227.concafe.domain.event.publisher.CastClaimEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CastClaimEventPublisherImpl : CastClaimEventPublisher {
    private val _events = MutableSharedFlow<CastClaimEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: CastClaimEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<CastClaimEvent> = _events.asSharedFlow()
}