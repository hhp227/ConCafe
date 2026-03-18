package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.ReviewEvent
import com.hhp227.concafe.domain.event.publisher.ReviewEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ReviewEventPublisherImpl : ReviewEventPublisher {
    private val _events = MutableSharedFlow<ReviewEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: ReviewEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<ReviewEvent> {
        return _events.asSharedFlow()
    }
}