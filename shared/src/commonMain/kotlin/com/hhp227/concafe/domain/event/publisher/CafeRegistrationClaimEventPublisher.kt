package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CafeRegistrationClaimEventPublisher {
    private val _events = MutableSharedFlow<CafeRegistrationClaimEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: CafeRegistrationClaimEvent) {
        _events.tryEmit(event)
    }
}