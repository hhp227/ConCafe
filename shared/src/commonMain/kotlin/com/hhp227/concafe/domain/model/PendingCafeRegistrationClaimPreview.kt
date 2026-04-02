package com.hhp227.concafe.domain.model

data class PendingCafeRegistrationClaimPreview(
    val claimId: String,
    val requesterUserId: String,
    val requesterNickname: String,
    val approvedCafeId: String?,
    val cafeName: String,
    val location: String,
    val requestedAt: String,
    val message: String,
    val imageUrl: String?
)
