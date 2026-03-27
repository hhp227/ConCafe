package com.hhp227.concafe.domain.model

data class CastClaim(
    val id: String,
    val userId: String,
    val cafeId: String,
    val castId: String,
    val castName: String = "",
    val status: CastClaimStatus,
    val message: String?,
    val evidenceImageUrls: List<String> = emptyList(),
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String,
    val createdAtLabel: String
)

enum class CastClaimStatus {
    PENDING,
    APPROVED,
    REJECTED
}
