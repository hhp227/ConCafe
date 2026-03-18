package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CafeDetailEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface CafeDetailEventPublisher {
    fun publish(event: CafeDetailEvent)

    @NativeCoroutines
    fun observe(): Flow<CafeDetailEvent>
}