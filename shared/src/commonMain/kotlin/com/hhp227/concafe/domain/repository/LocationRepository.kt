package com.hhp227.concafe.domain.repository

import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.LocationPermissionStatus

interface LocationRepository {
    suspend fun requestPermission(): LocationPermissionStatus

    suspend fun getCurrentLocation(): CurrentLocation

    suspend fun getLastKnownLocation(): GeoPoint?
}
