package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun CheckInQrScanner(
    onScanSuccess: (String) -> Unit,
    onScanCanceled: () -> Unit,
    onScanFailed: (String) -> Unit,
    modifier: Modifier
) {
    Text("QR 스캔은 모바일에서만 지원됩니다.", modifier = modifier)
}

