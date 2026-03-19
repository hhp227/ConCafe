package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.CafeRegistrationDraft
import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import kotlinx.coroutines.flow.Flow

interface CafeRegistrationClaimRepository {
    suspend fun createCafeRegistrationClaim(userId: String, draft: CafeRegistrationDraft): PendingCafeRegistrationClaimPreview

    suspend fun getPendingCafeRegistrationClaims(): List<PendingCafeRegistrationClaimPreview>

    suspend fun approveCafeRegistrationClaim(claimId: String, reviewedBy: String): PendingCafeRegistrationClaimPreview

    suspend fun rejectCafeRegistrationClaim(claimId: String, reviewedBy: String): PendingCafeRegistrationClaimPreview
}
