package com.hhp227.concafe.domain.event

sealed class CafeRegistrationClaimEvent {
    data class Created(val requesterUserId: String, val claimId: String) : CafeRegistrationClaimEvent()

    data class Approved(val requesterUserId: String, val claimId: String) : CafeRegistrationClaimEvent()

    data class Rejected(val requesterUserId: String, val claimId: String) : CafeRegistrationClaimEvent()
}