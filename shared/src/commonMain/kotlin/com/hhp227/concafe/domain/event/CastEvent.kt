package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.Cast

sealed class CastEvent {
    data class Created(val cafeId: String, val cast: Cast) : CastEvent()

    data class Updated(
        val cafeId: String,
        val cast: Cast,
        val isFollowing: Boolean? = null
    ) : CastEvent()

    data class Deleted(val cafeId: String, val castId: String) : CastEvent()
}
