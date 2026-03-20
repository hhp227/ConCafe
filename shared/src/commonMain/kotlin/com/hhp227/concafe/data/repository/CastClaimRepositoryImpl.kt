package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.common.PagedResult
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.CastClaimCandidate
import com.hhp227.concafe.domain.model.MyCastClaimStatus
import com.hhp227.concafe.domain.model.PendingCastClaimPreview
import com.hhp227.concafe.domain.repository.CastClaimRepository

class CastClaimRepositoryImpl : CastClaimRepository {
    override suspend fun getAffiliatedCafeId(userId: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getMyCastClaimStatus(userId: String): MyCastClaimStatus {
        TODO("Not yet implemented")
    }

    override suspend fun getMyRequestableCastPage(
        userId: String,
        cursor: String?,
        pageSize: Int
    ): PagedResult<CastClaimCandidate> {
        TODO("Not yet implemented")
    }

    override suspend fun getPendingCastClaimsForCafe(cafeId: String): List<PendingCastClaimPreview> {
        TODO("Not yet implemented")
    }

    override suspend fun createCastClaim(
        userId: String,
        cafeId: String,
        castId: String,
        message: String?
    ): CastClaim {
        TODO("Not yet implemented")
    }

    override suspend fun approveCastClaim(
        claimId: String,
        reviewedBy: String
    ): CastClaim {
        TODO("Not yet implemented")
    }

    override suspend fun rejectCastClaim(
        claimId: String,
        reviewedBy: String
    ): CastClaim {
        TODO("Not yet implemented")
    }
}