package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import kotlin.math.roundToInt

@Composable
actual fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    onCafeCheckIn: (String) -> Unit,
    showCheckInButton: Boolean,
    cameraTarget: CheckInMapCameraTarget?,
    modifier: Modifier
) {
    val cameraState = rememberCameraPositionState()
    var selectedCafe by remember { mutableStateOf<CheckInCafeSummary?>(null) }
    var calloutWidthPx by remember { mutableIntStateOf(0) }
    var calloutHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(cafes, cameraTarget) {
        val targetPosition = resolveCheckInMapCameraPosition(
            cafes = cafes,
            cameraTarget = cameraTarget
        )

        cameraState.move(CameraUpdateFactory.newCameraPosition(targetPosition))
    }
    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            onMapClick = { selectedCafe = null }
        )
        val cameraPosition = cameraState.position
        val isCameraMoving = cameraState.isMoving

        cafes.forEach { cafe ->
            val projection = cameraState.projection

            if (projection != null) {
                val screenPoint = projection.toScreenLocation(
                    LatLng(cafe.geoPoint.latitude, cafe.geoPoint.longitude)
                )
                val labelWidthPx = with(density) { MARKER_LABEL_WIDTH_DP.dp.toPx() }.roundToInt()
                val labelTopMarginPx = with(density) { 2.dp.toPx() }.roundToInt()
                val labelX = screenPoint.x - labelWidthPx / 2
                val labelY = screenPoint.y + labelTopMarginPx

                CafeMarkerLabel(
                    cafeName = cafe.name.toMarkerLabel(),
                    modifier = Modifier.absoluteOffset {
                        cameraPosition
                        isCameraMoving
                        IntOffset(labelX, labelY)
                    }
                )
            }
        }
        cafes.forEach { cafe ->
            val projection = cameraState.projection

            if (projection != null) {
                val screenPoint = projection.toScreenLocation(
                    LatLng(cafe.geoPoint.latitude, cafe.geoPoint.longitude)
                )
                val pinWidthPx = with(density) { MARKER_PIN_WIDTH_DP.dp.toPx() }.roundToInt()
                val pinHeightPx = with(density) { MARKER_PIN_HEIGHT_DP.dp.toPx() }.roundToInt()
                val pinX = screenPoint.x - pinWidthPx / 2
                val pinY = screenPoint.y - pinHeightPx

                CafeMapPin(
                    modifier = Modifier
                        .absoluteOffset {
                            cameraPosition
                            isCameraMoving
                            IntOffset(pinX, pinY)
                        }
                        .size(MARKER_PIN_WIDTH_DP.dp, MARKER_PIN_HEIGHT_DP.dp),
                    onClick = {
                        selectedCafe = cafe
                    }
                )
            }
        }
        selectedCafe?.let { cafe ->
            val projection = cameraState.projection

            if (projection != null) {
                val screenPoint = projection.toScreenLocation(
                    LatLng(cafe.geoPoint.latitude, cafe.geoPoint.longitude)
                )
                val pinOffsetPx = with(density) { 34.dp.toPx() }.roundToInt()
                val calloutX = screenPoint.x - calloutWidthPx / 2
                val calloutY = screenPoint.y - calloutHeightPx - pinOffsetPx

                Box(
                    modifier = Modifier
                        .absoluteOffset { IntOffset(calloutX, calloutY) }
                        .onGloballyPositioned { coords ->
                            calloutWidthPx = coords.size.width
                            calloutHeightPx = coords.size.height
                        }
                ) {
                    CafeMapCallout(
                        cafeName = cafe.name,
                        onCafeClick = {
                            selectedCafe = null
                            onCafeClick(cafe.id)
                        },
                        onCheckIn = {
                            selectedCafe = null
                            onCafeCheckIn(cafe.id)
                        },
                        showCheckInButton = showCheckInButton
                    )
                }
            }
        }
    }
}

@Composable
private fun CafeMarkerLabel(
    cafeName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = cafeName,
        color = ConCafeColors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = modifier.width(MARKER_LABEL_WIDTH_DP.dp)
    )
}

@Composable
private fun CafeMapPin(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Canvas(
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures { tapOffset ->
                if (tapOffset.isInsideMarkerPin(size.width.toFloat(), size.height.toFloat())) {
                    onClick()
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val pinPath = ComposePath().apply {
            moveTo(w / 2f, h)
            cubicTo(w * 0.18f, h * 0.64f, 0f, h * 0.46f, 0f, h * 0.36f)
            cubicTo(0f, h * 0.15f, w * 0.18f, 0f, w / 2f, 0f)
            cubicTo(w * 0.82f, 0f, w, h * 0.15f, w, h * 0.36f)
            cubicTo(w, h * 0.46f, w * 0.82f, h * 0.64f, w / 2f, h)
            close()
        }

        drawPath(pinPath, ConCafeColors.primary)
        drawPath(
            path = pinPath,
            color = Color.White,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = w * 0.19f,
            center = Offset(w / 2f, h * 0.36f)
        )
    }
}

private fun Offset.isInsideMarkerPin(width: Float, height: Float): Boolean {
    val normalizedX = x / width
    val normalizedY = y / height
    val topCircleCenterY = 0.36f
    val topCircleRadius = 0.36f
    val circleDistanceX = (normalizedX - 0.5f) / topCircleRadius
    val circleDistanceY = (normalizedY - topCircleCenterY) / topCircleRadius
    val isInsideTopCircle = circleDistanceX * circleDistanceX + circleDistanceY * circleDistanceY <= 1f
    val isInsideLowerTip = normalizedY in 0.36f..1f &&
            kotlin.math.abs(normalizedX - 0.5f) <= (1f - normalizedY) * 0.5f
    return isInsideTopCircle || isInsideLowerTip
}

private fun String.toMarkerLabel(): String {
    val trimmed = trim()
    return if (trimmed.length >= MARKER_LABEL_ELLIPSIS_THRESHOLD) {
        trimmed.take(MARKER_LABEL_VISIBLE_CHARS) + MARKER_LABEL_ELLIPSIS
    } else {
        trimmed
    }
}

@Composable
private fun CafeMapCallout(
    cafeName: String,
    onCafeClick: () -> Unit,
    onCheckIn: () -> Unit,
    showCheckInButton: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = cafeName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = ConCafeColors.textPrimary,
                modifier = Modifier.clickable(onClick = onCafeClick)
            )
            if (showCheckInButton) {
                IconButton(
                    onClick = onCheckIn,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = "체크인",
                        tint = ConCafeColors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun resolveCheckInMapCameraPosition(
    cafes: List<CheckInCafeSummary>,
    cameraTarget: CheckInMapCameraTarget?
): CameraPosition {
    val defaultSeoul = LatLng(37.5665, 126.9780)

    if (cafes.isEmpty()) {
        if (cameraTarget != null) {
            return CameraPosition.fromLatLngZoom(
                LatLng(cameraTarget.latitude, cameraTarget.longitude),
                cameraTarget.zoom + CHECK_IN_MAP_ZOOM_IN_STEP
            )
        }
        return CameraPosition.fromLatLngZoom(defaultSeoul, 11.5f + CHECK_IN_MAP_ZOOM_IN_STEP)
    }

    if (cafes.size == 1) {
        val first = cafes.first()
        return CameraPosition.fromLatLngZoom(
            LatLng(first.geoPoint.latitude, first.geoPoint.longitude),
            14.5f + CHECK_IN_MAP_ZOOM_IN_STEP
        )
    }

    val boundsBuilder = LatLngBounds.builder()
    cafes.forEach { cafe ->
        boundsBuilder.include(LatLng(cafe.geoPoint.latitude, cafe.geoPoint.longitude))
    }

    val center = boundsBuilder.build().center

    return CameraPosition.fromLatLngZoom(center, 12.5f + CHECK_IN_MAP_ZOOM_IN_STEP)
}

private const val CHECK_IN_MAP_ZOOM_IN_STEP = 1.0f
private const val MARKER_PIN_WIDTH_DP = 20
private const val MARKER_PIN_HEIGHT_DP = 29
private const val MARKER_LABEL_WIDTH_DP = 80
private const val MARKER_LABEL_ELLIPSIS_THRESHOLD = 7
private const val MARKER_LABEL_VISIBLE_CHARS = 6
private const val MARKER_LABEL_ELLIPSIS = "…"
