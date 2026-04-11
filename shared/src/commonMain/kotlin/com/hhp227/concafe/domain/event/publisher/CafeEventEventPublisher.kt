package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CafeEventEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CafeEventEventPublisher {
    private val _events = MutableSharedFlow<CafeEventEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: CafeEventEvent) {
        _events.tryEmit(event)
    }
}
