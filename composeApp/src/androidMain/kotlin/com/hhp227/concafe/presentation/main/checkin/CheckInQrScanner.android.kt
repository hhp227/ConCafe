package com.hhp227.concafe.presentation.main.checkin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
actual fun CheckInQrScanner(
    onScanSuccess: (String) -> Unit,
    onScanCanceled: () -> Unit,
    onScanFailed: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var started by remember { mutableStateOf(false) }

    if (activity == null) {
        Text("QR 스캔을 시작할 수 없습니다.", modifier = modifier)
        return
    }

    val scanner = remember(activity) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(activity, options)
    }

    LaunchedEffect(scanner) {
        if (started) return@LaunchedEffect
        started = true
        scanner
            .startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue?.trim().orEmpty()
                if (rawValue.isBlank()) {
                    onScanFailed("QR 코드를 인식하지 못했습니다.")
                } else {
                    onScanSuccess(rawValue)
                }
            }
            .addOnCanceledListener {
                onScanCanceled()
            }
            .addOnFailureListener { error ->
                val apiError = error as? ApiException
                if (apiError?.statusCode == CommonStatusCodes.CANCELED) {
                    onScanCanceled()
                } else {
                    onScanFailed("QR 스캔에 실패했습니다.")
                }
            }
    }

    CircularProgressIndicator(modifier = modifier)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
