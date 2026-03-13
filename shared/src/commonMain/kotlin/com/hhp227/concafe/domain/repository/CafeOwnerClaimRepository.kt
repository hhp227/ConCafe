package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview

interface CafeOwnerClaimRepository {
    suspend fun createCafeOwnerClaim(userId: String, cafeId: String): PendingCafeOwnerClaimPreview

    suspend fun getPendingCafeOwnerClaims(): List<PendingCafeOwnerClaimPreview>

    suspend fun approveCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview

    suspend fun rejectCafeOwnerClaim(claimId: String, reviewedBy: String): PendingCafeOwnerClaimPreview
}
