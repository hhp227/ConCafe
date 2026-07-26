package com.hhp227.concafe.presentation.main.checkin

data class CheckInCurrentLocation(
    val latitude: Double,
    val longitude: Double
)

sealed interface CheckInLocationResult {
    data class Success(val location: CheckInCurrentLocation) : CheckInLocationResult

    data class Failure(val message: String) : CheckInLocationResult
}

sealed interface CheckInLocationPermissionResult {
    data object Granted : CheckInLocationPermissionResult

    data class Failure(
        val message: String,
        val requiresSettings: Boolean = false
    ) : CheckInLocationPermissionResult
}

interface CheckInLocationProvider {
    suspend fun requestPermissionIfNeeded(): CheckInLocationPermissionResult

    suspend fun getCurrentLocation(): CheckInLocationResult
}
