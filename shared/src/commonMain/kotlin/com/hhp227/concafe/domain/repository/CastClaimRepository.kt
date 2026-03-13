package com.hhp227.concafe.domain.repository

import kotlinx.coroutines.flow.Flow
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimEvent
import com.hhp227.concafe.domain.model.MyCastClaimStatus
import com.hhp227.concafe.domain.model.PendingCastClaimPreview

interface CastClaimRepository {
    fun observeCastClaimEvent(): Flow<CastClaimEvent>

    suspend fun getAffiliatedCafeId(userId: String): String?

    suspend fun getMyCastClaimStatus(userId: String): MyCastClaimStatus

    suspend fun getPendingCastClaimsForCafe(cafeId: String): List<PendingCastClaimPreview>

    suspend fun createCastClaim(userId: String, cafeId: String, castId: String, message: String?): CastClaim

    suspend fun approveCastClaim(claimId: String, reviewedBy: String): CastClaim

    suspend fun rejectCastClaim(claimId: String, reviewedBy: String): CastClaim
}
