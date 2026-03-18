package com.hhp227.concafe.dataevent.publisher

import com.hhp227.concafe.domain.event.BannerEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BannerEventPublisherImpl : BannerEventPublisher {
    private val _events = MutableSharedFlow<BannerEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun publish(event: BannerEvent) {
        _events.tryEmit(event)
    }

    override fun observe(): Flow<BannerEvent> = _events.asSharedFlow()
}