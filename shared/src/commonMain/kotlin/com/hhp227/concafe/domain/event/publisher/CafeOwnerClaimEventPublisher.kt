package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CafeOwnerClaimEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CafeOwnerClaimEventPublisher {
    private val _events = MutableSharedFlow<CafeOwnerClaimEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: CafeOwnerClaimEvent) {
        _events.tryEmit(event)
    }
}
