package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.hhp227.concafe.domain.model.CheckInCafeSummary

@Composable
actual fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    modifier: Modifier
) {
    val cameraState = rememberCheckInMapCameraState(cafes)

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState
    ) {
        cafes.forEach { cafe ->
            val markerPosition = LatLng(
                cafe.geoPoint.latitude,
                cafe.geoPoint.longitude
            )

            Marker(
                state = MarkerState(position = markerPosition),
                title = cafe.name,
                snippet = cafe.locationLabel,
                onClick = {
                    onCafeClick(cafe.id)
                    true
                }
            )
        }
    }
}

@Composable
private fun rememberCheckInMapCameraState(cafes: List<CheckInCafeSummary>): CameraPositionState {
    val defaultSeoul = LatLng(37.5665, 126.9780)
    val cameraPosition = remember(cafes) {
        if (cafes.isEmpty()) {
            CameraPosition.fromLatLngZoom(defaultSeoul, 11.5f)
        } else if (cafes.size == 1) {
            val first = cafes.first()
            CameraPosition.fromLatLngZoom(
                LatLng(first.geoPoint.latitude, first.geoPoint.longitude),
                14.5f
            )
        } else {
            val boundsBuilder = LatLngBounds.builder()
            cafes.forEach { cafe ->
                boundsBuilder.include(LatLng(cafe.geoPoint.latitude, cafe.geoPoint.longitude))
            }
            val center = boundsBuilder.build().center
            CameraPosition.fromLatLngZoom(center, 12.5f)
        }
    }
    return rememberCameraPositionState {
        position = cameraPosition
    }
}
