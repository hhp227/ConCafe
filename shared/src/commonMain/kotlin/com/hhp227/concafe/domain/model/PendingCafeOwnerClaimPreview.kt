package com.hhp227.concafe.domain.model

data class PendingCafeOwnerClaimPreview(
    val claimId: String,
    val requesterUserId: String,
    val requesterNickname: String,
    val cafeId: String,
    val cafeName: String,
    val location: String,
    val requestedAt: String,
    val message: String,
    val imageUrl: String? = null
)
