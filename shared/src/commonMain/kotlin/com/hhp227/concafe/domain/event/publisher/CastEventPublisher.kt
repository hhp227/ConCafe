package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CastEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface CastEventPublisher {
    fun publish(event: CastEvent)

    @NativeCoroutines
    fun observe(): Flow<CastEvent>
}