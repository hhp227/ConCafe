package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.repository.CafeOwnerClaimRepository

class CafeOwnerClaimRepositoryImpl : CafeOwnerClaimRepository {
    override suspend fun createCafeOwnerClaim(
        userId: String,
        cafeId: String
    ): PendingCafeOwnerClaimPreview {
        TODO("Not yet implemented")
    }

    override suspend fun getPendingCafeOwnerClaims(): List<PendingCafeOwnerClaimPreview> {
        TODO("Not yet implemented")
    }

    override suspend fun approveCafeOwnerClaim(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview {
        TODO("Not yet implemented")
    }

    override suspend fun rejectCafeOwnerClaim(
        claimId: String,
        reviewedBy: String
    ): PendingCafeOwnerClaimPreview {
        TODO("Not yet implemented")
    }
}