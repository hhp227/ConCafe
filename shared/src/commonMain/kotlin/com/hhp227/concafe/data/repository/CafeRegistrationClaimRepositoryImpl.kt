package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.CafeRegistrationDraft
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.repository.CafeRegistrationClaimRepository

class CafeRegistrationClaimRepositoryImpl : CafeRegistrationClaimRepository {
    override suspend fun createCafeRegistrationClaim(
        userId: String,
        draft: CafeRegistrationDraft
    ): PendingCafeRegistrationClaimPreview {
        TODO("Not yet implemented")
    }

    override suspend fun getPendingCafeRegistrationClaims(): List<PendingCafeRegistrationClaimPreview> {
        TODO("Not yet implemented")
    }

    override suspend fun approveCafeRegistrationClaim(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview {
        TODO("Not yet implemented")
    }

    override suspend fun rejectCafeRegistrationClaim(
        claimId: String,
        reviewedBy: String
    ): PendingCafeRegistrationClaimPreview {
        TODO("Not yet implemented")
    }
}