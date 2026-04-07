package com.hhp227.concafe.domain.model

data class CafeDashboardData(
    val id: String,
    val name: String,
    val city: String,
    val todayCheckIns: Int,
    val todayReviews: Int,
    val rating: Double,
    val castPreviews: List<CastPreview>,
    val homeBannerPreview: HomeBannerPreview,
    val socialMedia: Map<String, String> = emptyMap(),
    val reservationUrl: String? = null
) {
    data class CastPreview(
        val id: String,
        val name: String,
        val isOnShift: Boolean
    )

    data class HomeBannerPreview(
        val title: String,
        val period: String,
        val statusLabel: String,
        val imageUrl: String? = null
    )
}
