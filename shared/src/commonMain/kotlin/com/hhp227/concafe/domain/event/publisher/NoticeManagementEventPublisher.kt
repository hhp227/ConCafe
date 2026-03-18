package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NoticeManagementEventPublisher {
    private val _events = MutableSharedFlow<NoticeManagementEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    @NativeCoroutines
    val events = _events.asSharedFlow()

    fun publish(event: NoticeManagementEvent) {
        _events.tryEmit(event)
    }
}