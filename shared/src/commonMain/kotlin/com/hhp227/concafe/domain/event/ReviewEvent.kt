package com.hhp227.concafe.domain.event

sealed class ReviewEvent {
    data class Created(val cafeId: String) : ReviewEvent()

    data class Deleted(val cafeId: String, val reviewId: String) : ReviewEvent()
}