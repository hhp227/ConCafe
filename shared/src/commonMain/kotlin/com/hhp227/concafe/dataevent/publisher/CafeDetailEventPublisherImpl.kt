package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.hhp227.concafe.domain.event.publisher.CafeDetailEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CafeDetailEventPublisherImpl : CafeDetailEventPublisher {
    private val _events = MutableSharedFlow<CafeDetailEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: CafeDetailEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<CafeDetailEvent> = _events.asSharedFlow()
}