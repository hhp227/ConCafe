package com.hhp227.concafe.domain.event

sealed class CafeOwnerClaimEvent {
    data class Created(val requesterUserId: String, val claimId: String) : CafeOwnerClaimEvent()

    data class Approved(val requesterUserId: String, val claimId: String) : CafeOwnerClaimEvent()

    data class Rejected(val requesterUserId: String, val claimId: String) : CafeOwnerClaimEvent()
}
