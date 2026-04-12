package com.hhp227.concafe.presentation.main.cafemanagement

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CafeManagementQrCode(
    payload: String,
    modifier: Modifier = Modifier
)
