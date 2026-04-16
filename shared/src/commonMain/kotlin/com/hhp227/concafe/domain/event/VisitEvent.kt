package com.hhp227.concafe.domain.event

sealed class VisitEvent {
    data class Created(val checkInId: String, val cafeId: String) : VisitEvent()
    data class Deleted(val checkInId: String) : VisitEvent()
}