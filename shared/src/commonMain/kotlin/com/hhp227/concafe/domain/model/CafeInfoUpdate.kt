package com.hhp227.concafe.domain.model

data class CafeInfoUpdate(
    val cafeId: String,
    val name: String,
    val description: String,
    val representativeImageUrl: String? = null,
    val galleryImages: List<String> = emptyList(),
    val location: GeoPoint? = null,
    val address: String,
    val contactNumber: String,
    val weekdayOpen: String,
    val weekdayClose: String,
    val weekendOpen: String,
    val weekendClose: String
)
