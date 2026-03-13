package com.hhp227.concafe.domain.model

sealed class CastEvent {
    data class Created(val cafeId: String, val castId: String) : CastEvent()

    data class Updated(val cafeId: String, val castId: String) : CastEvent()

    data class Deleted(val cafeId: String, val castId: String) : CastEvent()
}
