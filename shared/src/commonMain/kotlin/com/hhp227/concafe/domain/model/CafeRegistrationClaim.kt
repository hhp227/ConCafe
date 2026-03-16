package com.hhp227.concafe.domain.model

data class CafeRegistrationClaim(
    val claimId: String,
    val cafeName: String,
    val description: String,
    val region: Region,
    val thumbnailImage: String?,
    val conceptType: String,
    val businessHours: String,
    val phoneNumber: String,
    val requestedAt: String,
    val status: String,
    val message: String
)
