package com.hhp227.concafe.data.source.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hhp227.concafe.domain.model.CurrentLocation
import com.hhp227.concafe.domain.model.GeoPoint
import com.hhp227.concafe.domain.model.LocationFailureReason
import com.hhp227.concafe.domain.model.LocationPermissionStatus
import com.hhp227.concafe.domain.policy.CheckInLocationPolicy
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class AndroidDeviceLocationDataSource(
    private val context: Context,
    private val activityProvider: () -> Activity?
) : DeviceLocationDataSource {
    private val locationPolicy = CheckInLocationPolicy()

    private var hasRequestedLocationPermission = false

    // The permission dialog is fire-and-forget: the result is not awaited, so the first call
    // reports REQUEST_PENDING and a later call observes the granted state.
    override suspend fun requestPermission(): LocationPermissionStatus {
        if (hasFineLocationPermission()) {
            return LocationPermissionStatus.GRANTED
        }
        val activity = activityProvider()
            ?: return LocationPermissionStatus.PRECISE_LOCATION_REQUIRED

        if (shouldOpenSettings(activity)) {
            return LocationPermissionStatus.PRECISE_LOCATION_REQUIRED
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
        return LocationPermissionStatus.REQUEST_PENDING
    }

    override suspend fun getCurrentLocation(): CurrentLocation {
        if (!hasFineLocationPermission()) {
            return CurrentLocation.Unavailable(LocationFailureReason.PERMISSION_REQUIRED)
        }
        val locationManager = locationManager()
            ?: return CurrentLocation.Unavailable(LocationFailureReason.SERVICE_UNAVAILABLE)
        val locations = listOfNotNull(
            requestRealtimeLocation(locationManager),
            resolveLastKnownLocation(locationManager)
        )
        val bestLocation = resolveBestLocation(locations)
        return if (bestLocation != null) {
            CurrentLocation.Available(bestLocation.toGeoPoint())
        } else {
            CurrentLocation.Unavailable(LocationFailureReason.LOW_ACCURACY)
        }
    }

    override suspend fun getLastKnownLocation(): GeoPoint? {
        if (!hasFineLocationPermission()) {
            return null
        }
        val locationManager = locationManager() ?: return null
        return resolveLastKnownLocation(locationManager)?.toGeoPoint()
    }

    private fun locationManager(): LocationManager? {
        return context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
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
        return withTimeoutOrNull(REALTIME_LOCATION_TIMEOUT_MILLIS) {
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
        val now = System.currentTimeMillis()
        val candidates = locations.filter { location ->
            location.hasAccuracy() && locationPolicy.isAcceptable(
                accuracyMeters = location.accuracy.toDouble(),
                ageMillis = now - location.time
            )
        }
        return candidates.minWithOrNull(
            compareBy<Location> { it.accuracy }
                .thenByDescending { it.time }
        )
    }

    private fun Location.toGeoPoint(): GeoPoint {
        return GeoPoint(latitude = latitude, longitude = longitude)
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 7001
        const val REALTIME_LOCATION_TIMEOUT_MILLIS = 10_000L
    }
}
