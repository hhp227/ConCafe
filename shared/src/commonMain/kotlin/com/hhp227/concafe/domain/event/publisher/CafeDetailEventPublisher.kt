package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class CafeDetailEventPublisher {
    private val _events = MutableSharedFlow<CafeDetailEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events: SharedFlow<CafeDetailEvent> = _events.asSharedFlow()

    fun publish(event: CafeDetailEvent) {
        _events.tryEmit(event)
    }
}
