package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.BannerEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface BannerEventPublisher {
    fun publish(event: BannerEvent)

    @NativeCoroutines
    fun observe(): Flow<BannerEvent>
}