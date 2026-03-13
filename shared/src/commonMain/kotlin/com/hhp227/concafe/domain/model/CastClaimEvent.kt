package com.hhp227.concafe.domain.model

sealed interface CastClaimEvent {
    data class Created(val claim: CastClaim) : CastClaimEvent
    data class Updated(val claim: CastClaim) : CastClaimEvent
}
