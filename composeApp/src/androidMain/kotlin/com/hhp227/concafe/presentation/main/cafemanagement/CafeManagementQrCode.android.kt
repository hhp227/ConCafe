package com.hhp227.concafe.presentation.main.cafemanagement

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@Composable
actual fun CafeManagementQrCode(
    payload: String,
    modifier: Modifier
) {
    val qrBitmap = remember(payload) {
        runCatching { createQrBitmap(payload) }.getOrNull()
    }
    qrBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

private fun createQrBitmap(payload: String): Bitmap {
    val bitMatrix = MultiFormatWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        QR_SIZE_PX,
        QR_SIZE_PX
    )
    val bitmap = createBitmap(QR_SIZE_PX, QR_SIZE_PX)

    for (x in 0 until QR_SIZE_PX) {
        for (y in 0 until QR_SIZE_PX) {
            bitmap[x, y] = if (bitMatrix[x, y]) BLACK else WHITE
        }
    }
    return bitmap
}

private const val QR_SIZE_PX = 1024
private const val BLACK = -0x1000000
private const val WHITE = -0x1
