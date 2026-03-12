package com.hhp227.concafe.domain.model

data class CafeManagementData(
    val ownedCafes: List<OwnedCafeSummary>,
    val searchableCafes: List<SearchableCafeSummary>,
    val pendingClaims: List<PendingClaimSummary>
) {
    data class OwnedCafeSummary(
        val id: String,
        val name: String,
        val city: String,
        val isApproved: Boolean,
        val todayVisitors: Int,
        val todayCheckIns: Int,
        val todayReviews: Int,
        val rating: Double,
        val castCount: Int,
        val noticeCount: Int,
        val externalLinkCount: Int
    )

    data class SearchableCafeSummary(
        val id: String,
        val name: String,
        val location: String
    )

    data class PendingClaimSummary(
        val cafeName: String,
        val requestedAt: String,
        val status: String,
        val message: String
    )
}
