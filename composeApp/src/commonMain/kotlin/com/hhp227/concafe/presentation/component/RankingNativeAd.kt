package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hhp227.concafe.data.model.NativeAdHandle

@Composable
expect fun RankingNativeAd(
    modifier: Modifier = Modifier,
    nativeAdHandle: NativeAdHandle?
)
