package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.hhp227.concafe.domain.event.publisher.NoticeManagementEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NoticeManagementEventPublisherImpl : NoticeManagementEventPublisher {
    private val _events = MutableSharedFlow<NoticeManagementEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: NoticeManagementEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<NoticeManagementEvent> = _events.asSharedFlow()
}