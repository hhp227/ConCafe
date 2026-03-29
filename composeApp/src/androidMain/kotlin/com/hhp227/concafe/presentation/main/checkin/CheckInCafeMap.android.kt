package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.hhp227.concafe.domain.model.CheckInCafeSummary

@Composable
actual fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    cameraTarget: CheckInMapCameraTarget?,
    modifier: Modifier
) {
    val cameraState = rememberCameraPositionState()

    LaunchedEffect(cafes, cameraTarget) {
        val targetPosition = resolveCheckInMapCameraPosition(
            cafes = cafes,
            cameraTarget = cameraTarget
        )

        cameraState.move(CameraUpdateFactory.newCameraPosition(targetPosition))
    }
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

private fun resolveCheckInMapCameraPosition(
    cafes: List<CheckInCafeSummary>,
    cameraTarget: CheckInMapCameraTarget?
): CameraPosition {
    if (cameraTarget != null) {
        return CameraPosition.fromLatLngZoom(
            LatLng(cameraTarget.latitude, cameraTarget.longitude),
            cameraTarget.zoom
        )
    }

    val defaultSeoul = LatLng(37.5665, 126.9780)

    if (cafes.isEmpty()) {
        return CameraPosition.fromLatLngZoom(defaultSeoul, 11.5f)
    }

    if (cafes.size == 1) {
        val first = cafes.first()

        return CameraPosition.fromLatLngZoom(
            LatLng(first.geoPoint.latitude, first.geoPoint.longitude),
            14.5f
        )
    }

    val boundsBuilder = LatLngBounds.builder()
    cafes.forEach { cafe ->
        boundsBuilder.include(LatLng(cafe.geoPoint.latitude, cafe.geoPoint.longitude))
    }

    val center = boundsBuilder.build().center

    return CameraPosition.fromLatLngZoom(center, 12.5f)
}
