package com.hhp227.concafe.domain.model

data class CheckInGuestFeed(
    val currentLocationLabel: String,
    val mapCafes: List<CheckInCafeSummary>,
    val popularCafes: List<CheckInCafeSummary>,
    val popularCasts: List<CheckInCastSummary>
)

data class CheckInCafeSummary(
    val id: String,
    val name: String,
    val locationLabel: String,
    val geoPoint: GeoPoint,
    val rating: Double,
    val checkInCount: Int
)

data class CheckInCastSummary(
    val id: String,
    val cafeId: String,
    val cafeName: String,
    val name: String,
    val profileImage: String?,
    val todayVisit: Int
)
