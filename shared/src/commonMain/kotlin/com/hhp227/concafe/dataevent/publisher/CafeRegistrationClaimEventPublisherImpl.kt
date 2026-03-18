package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CafeRegistrationClaimEventPublisherImpl : CafeRegistrationClaimEventPublisher {
    private val _events = MutableSharedFlow<CafeRegistrationClaimEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: CafeRegistrationClaimEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<CafeRegistrationClaimEvent> = _events.asSharedFlow()
}