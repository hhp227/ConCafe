package com.hhp227.concafe.data.source.location

import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.LocationPermissionStatus

/**
 * Device location service. [requestPermission] may show the system permission dialog;
 * [getCurrentLocation] returns a fix that satisfies CheckInLocationPolicy or the reason it
 * could not; [getLastKnownLocation] is a best-effort cached fix without waiting for hardware.
 */
interface DeviceLocationDataSource {
    suspend fun requestPermission(): LocationPermissionStatus

    suspend fun getCurrentLocation(): CurrentLocation

    suspend fun getLastKnownLocation(): GeoPoint?
}
