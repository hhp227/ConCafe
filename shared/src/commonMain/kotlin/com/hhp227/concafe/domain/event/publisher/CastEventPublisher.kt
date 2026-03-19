package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CastEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CastEventPublisher {
    private val _events = MutableSharedFlow<CastEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: CastEvent) {
        _events.tryEmit(event)
    }
}