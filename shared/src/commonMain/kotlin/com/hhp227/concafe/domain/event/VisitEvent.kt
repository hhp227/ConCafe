package com.hhp227.concafe.domain.event

sealed class VisitEvent {
    data class Created(val checkInId: String) : VisitEvent()
    data class Deleted(val checkInId: String) : VisitEvent()
}