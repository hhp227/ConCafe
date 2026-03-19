package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.ReviewEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ReviewEventPublisher {
    private val _events = MutableSharedFlow<ReviewEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: ReviewEvent) {
        _events.tryEmit(event)
    }
}