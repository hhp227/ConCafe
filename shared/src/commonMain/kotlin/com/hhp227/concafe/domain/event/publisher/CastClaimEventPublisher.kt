package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CastClaimEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface CastClaimEventPublisher {
    fun publish(event: CastClaimEvent)

    @NativeCoroutines
    fun observe(): Flow<CastClaimEvent>
}