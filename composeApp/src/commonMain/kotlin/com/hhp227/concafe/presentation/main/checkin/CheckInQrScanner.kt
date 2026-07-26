package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CheckInQrScanner(
    onScanSuccess: (String) -> Unit,
    onScanCanceled: () -> Unit,
    onScanFailed: (String) -> Unit,
    modifier: Modifier = Modifier
)

