package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hhp227.concafe.domain.model.CheckInCafeSummary

data class CheckInMapCameraTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float
)

@Composable
expect fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    onCafeCheckIn: (String) -> Unit = {},
    cameraTarget: CheckInMapCameraTarget? = null,
    modifier: Modifier = Modifier
)
