package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.CafeEventManagementItem

sealed class CafeEventEvent {
    data class Created(
        val cafeId: String,
        val event: CafeEventManagementItem
    ) : CafeEventEvent()

    data class Updated(
        val cafeId: String,
        val event: CafeEventManagementItem
    ) : CafeEventEvent()

    data class Deleted(
        val cafeId: String,
        val eventId: String
    ) : CafeEventEvent()
}
