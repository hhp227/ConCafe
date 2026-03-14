package com.hhp227.concafe.presentation.external

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ExternalLinkWebView(
    url: String,
    modifier: Modifier = Modifier
)
