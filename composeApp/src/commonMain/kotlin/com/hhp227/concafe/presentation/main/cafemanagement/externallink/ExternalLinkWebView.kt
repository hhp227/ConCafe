package com.hhp227.concafe.presentation.main.cafemanagement.externallink

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ExternalLinkWebView(
    url: String,
    modifier: Modifier = Modifier
)
