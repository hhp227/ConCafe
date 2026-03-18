package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface CafeRegistrationClaimEventPublisher {
    fun publish(event: CafeRegistrationClaimEvent)

    @NativeCoroutines
    fun observe(): Flow<CafeRegistrationClaimEvent>
}