package com.hhp227.concafe.domain.model

data class Region(
    val country: String,
    val city: String,
    val address: String,
    val location: GeoPoint
)
