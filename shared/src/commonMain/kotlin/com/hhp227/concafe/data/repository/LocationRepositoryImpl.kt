package com.hhp227.concafe.data.repository

import com.hhp227.concafe.data.source.location.DeviceLocationDataSource
import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.LocationPermissionStatus
import com.hhp227.concafe.domain.repository.LocationRepository

class LocationRepositoryImpl(
    private val deviceLocationDataSource: DeviceLocationDataSource
) : LocationRepository {
    override suspend fun requestPermission(): LocationPermissionStatus {
        return deviceLocationDataSource.requestPermission()
    }

    override suspend fun getCurrentLocation(): CurrentLocation {
        return deviceLocationDataSource.getCurrentLocation()
    }

    override suspend fun getLastKnownLocation(): GeoPoint? {
        return deviceLocationDataSource.getLastKnownLocation()
    }
}
