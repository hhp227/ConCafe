package com.hhp227.concafe.domain.event

import com.hhp227.concafe.domain.model.CastClaim

sealed class CastClaimEvent {
    data class Created(val claim: CastClaim) : CastClaimEvent()
    data class Updated(val claim: CastClaim) : CastClaimEvent()
}