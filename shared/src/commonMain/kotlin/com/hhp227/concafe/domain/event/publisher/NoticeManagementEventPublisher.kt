package com.hhp227.concafe.domain.event.publisher

import com.hhp227.concafe.domain.event.NoticeManagementEvent
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

interface NoticeManagementEventPublisher {
    fun publish(event: NoticeManagementEvent)

    @NativeCoroutines
    fun observe(): Flow<NoticeManagementEvent>
}