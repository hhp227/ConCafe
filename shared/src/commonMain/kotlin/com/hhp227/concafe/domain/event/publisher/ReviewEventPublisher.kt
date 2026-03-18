package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.ReviewEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface ReviewEventPublisher {
    fun publish(event: ReviewEvent)

    @NativeCoroutines
    fun observe(): Flow<ReviewEvent>
}