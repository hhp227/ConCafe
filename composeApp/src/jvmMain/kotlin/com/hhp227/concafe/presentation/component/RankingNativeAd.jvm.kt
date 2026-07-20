package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.data.model.NativeAdHandle

@Composable
actual fun RankingNativeAd(
    modifier: Modifier,
    nativeAdHandle: NativeAdHandle?
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ad",
            style = MaterialTheme.typography.labelLarge,
            color = ConCafeColors.textMuted
        )
    }
}

@Composable
actual fun CommunityNativeAd(
    modifier: Modifier,
    nativeAdHandle: NativeAdHandle?
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ad",
            style = MaterialTheme.typography.labelLarge,
            color = ConCafeColors.textMuted
        )
    }
}
