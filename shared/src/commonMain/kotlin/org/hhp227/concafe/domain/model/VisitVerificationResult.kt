package com.hhp227.concafe.domain.model

data class VisitVerificationResult(
    val verified: Boolean,
    val distanceMeters: Double,
    val allowedRadiusMeters: Double,
    val message: String
)
