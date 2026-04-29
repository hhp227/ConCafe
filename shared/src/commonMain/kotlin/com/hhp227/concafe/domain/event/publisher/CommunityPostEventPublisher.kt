package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CommunityPostEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CommunityPostEventPublisher {
    private val _events = MutableSharedFlow<CommunityPostEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: CommunityPostEvent) {
        _events.tryEmit(event)
    }
}
