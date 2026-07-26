package com.hhp227.concafe.presentation.main.checkin

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.CancellationSignal
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidCheckInLocationProvider(
    private val context: Context,
    private val activityProvider: () -> Activity?
) : CheckInLocationProvider {
    private var hasRequestedLocationPermission = false

    override suspend fun requestPermissionIfNeeded(): CheckInLocationPermissionResult {
        val hasPermission = hasFineLocationPermission()

        if (hasPermission) {
            return CheckInLocationPermissionResult.Granted
        } else {
            val activity = activityProvider()

            if (activity != null) {
                val shouldOpenSettings = shouldOpenSettings(activity)

                if (shouldOpenSettings) {
                    return CheckInLocationPermissionResult.Failure(
                        message = MSG_PRECISE_LOCATION_REQUIRED,
                        requiresSettings = true
                    )
                }
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                hasRequestedLocationPermission = true
                return CheckInLocationPermissionResult.Failure(
                    message = "위치 권한 요청 중입니다. 권한을 허용한 뒤 다시 시도해 주세요.",
                    requiresSettings = false
                )
            } else {
                return CheckInLocationPermissionResult.Failure(
                    message = MSG_PRECISE_LOCATION_REQUIRED,
                    requiresSettings = true
                )
            }
        }
    }

    override suspend fun getCurrentLocation(): CheckInLocationResult {
        if (!hasFineLocationPermission()) {
            return CheckInLocationResult.Failure(MSG_PRECISE_LOCATION_REQUIRED)
        }
        return resolveCurrentLocation()
    }

    private suspend fun resolveCurrentLocation(): CheckInLocationResult {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        if (locationManager == null) {
            return CheckInLocationResult.Failure("위치 서비스를 사용할 수 없습니다.")
        }
        val realtimeLocation = requestRealtimeLocation(locationManager)
        val locations = listOfNotNull(
            realtimeLocation,
            resolveLastKnownLocation(locationManager)
        )
        val bestLocation = resolveBestLocation(locations)
        return if (bestLocation != null) {
            CheckInLocationResult.Success(
                location = CheckInCurrentLocation(
                    latitude = bestLocation.latitude,
                    longitude = bestLocation.longitude
                )
            )
        } else {
            CheckInLocationResult.Failure(MSG_PRECISE_LOCATION_LOW_ACCURACY)
        }
    }

    private suspend fun requestRealtimeLocation(locationManager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider == null) {
            return null
        }
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()

                continuation.invokeOnCancellation {
                    cancellationSignal.cancel()
                }
                runCatching {
                    locationManager.getCurrentLocation(
                        provider,
                        cancellationSignal,
                        context.mainExecutor
                    ) { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                }.onFailure {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private fun resolveLastKnownLocation(locationManager: LocationManager): Location? {
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
        return resolveBestLocation(locations)
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun shouldOpenSettings(activity: Activity): Boolean {
        val shouldShowFineRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val shouldShowCoarseRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return hasRequestedLocationPermission && !shouldShowFineRationale && !shouldShowCoarseRationale
    }

    private fun resolveBestLocation(locations: List<Location>): Location? {
        if (locations.isEmpty()) {
            return null
        }
        val now = System.currentTimeMillis()
        val candidates = locations.filter { location ->
            val ageMillis = now - location.time
            val isRecent = ageMillis in 0..MAX_LOCATION_AGE_MILLIS
            val isAccurate = location.hasAccuracy() && location.accuracy in 0f..MAX_ALLOWED_ACCURACY_METERS

            isRecent && isAccurate
        }
        return candidates.minWithOrNull(
            compareBy<Location> { it.accuracy }
                .thenByDescending { it.time }
        )
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 7001
        const val MAX_LOCATION_AGE_MILLIS = 30_000L
        const val MAX_ALLOWED_ACCURACY_METERS = 80f
        const val MSG_PRECISE_LOCATION_REQUIRED = "정확한 위치 권한이 필요합니다. 설정에서 정확한 위치를 허용해 주세요."
        const val MSG_PRECISE_LOCATION_LOW_ACCURACY = "위치 정확도가 낮습니다. 정확한 위치를 켜고 잠시 후 다시 시도해 주세요."
    }
}
