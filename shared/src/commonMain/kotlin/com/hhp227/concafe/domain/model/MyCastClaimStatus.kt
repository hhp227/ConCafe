package com.hhp227.concafe.domain.model

data class MyCastClaimStatus(
    val userId: String,
    val affiliatedCafeId: String?,
    val affiliatedCafeName: String?,
    val linkedCastId: String?,
    val linkedCastName: String?,
    val pendingClaim: CastClaim?,
    val latestRejectedClaim: CastClaim?,
    val hasRequestableCasts: Boolean
) {
    val hasLinkedProfile: Boolean
        get() = linkedCastId != null
}

data class CastClaimCandidate(
    val castId: String,
    val castName: String
)

data class PendingCastClaimPreview(
    val claimId: String,
    val requesterUserId: String,
    val requesterNickname: String,
    val castId: String,
    val castName: String,
    val requestedAtLabel: String,
    val message: String?
)
