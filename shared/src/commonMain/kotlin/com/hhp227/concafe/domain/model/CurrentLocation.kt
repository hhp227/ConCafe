package com.hhp227.concafe.domain.model

enum class LocationFailureReason {
    PERMISSION_REQUIRED,
    SERVICE_UNAVAILABLE,
    LOW_ACCURACY,
    UNSUPPORTED
}

sealed interface CurrentLocation {
    data class Available(val point: GeoPoint) : CurrentLocation

    data class Unavailable(val reason: LocationFailureReason) : CurrentLocation
}
