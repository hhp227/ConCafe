package com.hhp227.concafe.presentation.main.checkin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AndroidCheckInLocationProvider(
    private val context: Context,
    private val activityProvider: () -> Activity?
) : CheckInLocationProvider {
    override suspend fun requestPermissionIfNeeded(): CheckInLocationPermissionResult {
        val hasPermission = hasLocationPermission()

        if (hasPermission) {
            return CheckInLocationPermissionResult.Granted
        } else {
            val activity = activityProvider()

            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                return CheckInLocationPermissionResult.Failure("위치 권한 요청 중입니다. 권한을 허용한 뒤 다시 시도해 주세요.")
            } else {
                return CheckInLocationPermissionResult.Failure("위치 권한이 필요합니다. 설정에서 위치 권한을 허용해 주세요.")
            }
        }
    }

    override suspend fun getCurrentLocation(): CheckInLocationResult {
        return when (val permissionResult = requestPermissionIfNeeded()) {
            CheckInLocationPermissionResult.Granted -> resolveCurrentLocation()
            is CheckInLocationPermissionResult.Failure -> CheckInLocationResult.Failure(permissionResult.message)
        }
    }

    private fun resolveCurrentLocation(): CheckInLocationResult {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        if (locationManager == null) {
            return CheckInLocationResult.Failure("위치 서비스를 사용할 수 없습니다.")
        }
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val locations = providers.mapNotNull { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else {
                    null
                }
            }.getOrNull()
        }
        val bestLocation = resolveBestLocation(locations)
        return if (bestLocation != null) {
            CheckInLocationResult.Success(
                location = CheckInCurrentLocation(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude
                )
            )
        } else {
            CheckInLocationResult.Failure("현재 위치를 확인할 수 없습니다. 위치 서비스를 켠 뒤 다시 시도해 주세요.")
        }
    }

    private fun hasLocationPermission(): Boolean {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFinePermission || hasCoarsePermission
    }

    private fun resolveBestLocation(locations: List<Location>): Location? {
        if (locations.isEmpty()) {
            return null
        }
        return locations.minByOrNull { location -> location.accuracy }
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 7001
    }
}
