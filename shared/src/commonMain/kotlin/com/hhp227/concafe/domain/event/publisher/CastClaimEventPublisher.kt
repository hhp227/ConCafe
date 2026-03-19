package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CastClaimEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CastClaimEventPublisher {
    private val _events = MutableSharedFlow<CastClaimEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: CastClaimEvent) {
        _events.tryEmit(event)
    }
}