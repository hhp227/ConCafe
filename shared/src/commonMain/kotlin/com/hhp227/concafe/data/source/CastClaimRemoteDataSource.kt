package com.hhp227.concafe.data.source

import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimStatus

interface CastClaimRemoteDataSource {
    suspend fun refreshCastClaimsForUser(userId: String)

    suspend fun fetchCastClaimsForUser(userId: String): List<CastClaim>

    suspend fun refreshCastClaimsForCafe(cafeId: String)

    suspend fun fetchCastClaimsForCafe(cafeId: String): List<CastClaim>

    suspend fun fetchAllCastClaims(): List<CastClaim>

    suspend fun hasCastClaimCafeSyncChanged(cafeId: String): Boolean

    suspend fun createCastClaimRemote(
        userId: String,
        cafeId: String,
        castId: String,
        message: String?
    ): CastClaim

    suspend fun updateCastClaimStatusRemote(
        claimId: String,
        reviewedBy: String,
        status: CastClaimStatus
    ): CastClaim
}
