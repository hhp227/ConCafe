package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.UserEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class UserEventPublisher {
    private val _events = MutableSharedFlow<UserEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: UserEvent) {
        _events.tryEmit(event)
    }
}
