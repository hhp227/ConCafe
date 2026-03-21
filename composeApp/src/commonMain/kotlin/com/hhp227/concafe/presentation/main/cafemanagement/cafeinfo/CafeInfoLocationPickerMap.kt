package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CafeInfoLocationPickerMap(
    latitude: Double,
    longitude: Double,
    onLocationSelected: (latitude: Double, longitude: Double, address: String?) -> Unit,
    modifier: Modifier = Modifier
)
