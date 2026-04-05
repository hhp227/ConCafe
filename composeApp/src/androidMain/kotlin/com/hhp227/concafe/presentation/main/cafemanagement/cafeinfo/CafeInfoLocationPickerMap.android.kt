package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder

@Composable
actual fun CafeInfoLocationPickerMap(
    latitude: Double,
    longitude: Double,
    onLocationSelected: (latitude: Double, longitude: Double, address: String?) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val selectedLocation = remember(latitude, longitude) {
        LatLng(latitude, longitude)
    }
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 15f)
    }

    LaunchedEffect(latitude, longitude) {
        cameraState.position = CameraPosition.fromLatLngZoom(selectedLocation, cameraState.position.zoom)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        properties = MapProperties(isMyLocationEnabled = false),
        onMapClick = { tapped ->
            val resolvedAddress = resolveAddressFromCoordinate(
                context = context,
                latitude = tapped.latitude,
                longitude = tapped.longitude
            )

            onLocationSelected(tapped.latitude, tapped.longitude, resolvedAddress)
        }
    ) {
        Marker(
            state = MarkerState(position = selectedLocation),
            title = "선택한 위치"
        )
    }
}

@SuppressLint("MissingPermission")
private fun resolveAddressFromCoordinate(
    context: Context,
    latitude: Double,
    longitude: Double
): String? {
    return runCatching {
        val geocoder = Geocoder(context, Locale.KOREA)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        addresses
            ?.firstOrNull()
            ?.getAddressLine(0)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
