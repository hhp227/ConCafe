package com.hhp227.concafe.data.source.location

import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.LocationFailureReason
import com.hhp227.concafe.domain.model.LocationPermissionStatus

class JvmDeviceLocationDataSource : DeviceLocationDataSource {
    override suspend fun requestPermission(): LocationPermissionStatus {
        return LocationPermissionStatus.UNSUPPORTED
    }

    override suspend fun getCurrentLocation(): CurrentLocation {
        return CurrentLocation.Unavailable(LocationFailureReason.UNSUPPORTED)
    }

    override suspend fun getLastKnownLocation(): GeoPoint? = null
}
