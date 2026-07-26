package com.hhp227.concafe.presentation.main.checkin

import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.checkin_location_mobile_only
import org.jetbrains.compose.resources.getString

class JvmCheckInLocationProvider : CheckInLocationProvider {
    override suspend fun requestPermissionIfNeeded(): CheckInLocationPermissionResult {
        return CheckInLocationPermissionResult.Failure(
            message = getString(Res.string.checkin_location_mobile_only),
            requiresSettings = false
        )
    }

    override suspend fun getCurrentLocation(): CheckInLocationResult {
        return CheckInLocationResult.Failure(getString(Res.string.checkin_location_mobile_only))
    }
}
