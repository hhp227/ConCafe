package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.BannerEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BannerEventPublisher {
    private val _events = MutableSharedFlow<BannerEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: BannerEvent) {
        _events.tryEmit(event)
    }
}