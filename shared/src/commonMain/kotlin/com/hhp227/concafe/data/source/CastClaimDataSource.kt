package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.CastClaim

interface CastClaimDataSource {
    val castClaims: MutableList<CastClaim>
}