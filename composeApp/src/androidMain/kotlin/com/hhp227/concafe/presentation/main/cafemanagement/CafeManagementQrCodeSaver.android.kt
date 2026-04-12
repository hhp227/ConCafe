package com.hhp227.concafe.presentation.main.cafemanagement

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@Composable
actual fun rememberCafeManagementQrCodeSaver(): suspend (String) -> Boolean {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        { payload ->
            withContext(Dispatchers.IO) {
                saveQrBitmapToGallery(
                    context = context,
                    bitmap = createQrBitmap(payload)
                )
            }
        }
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

private fun saveQrBitmapToGallery(
    context: Context,
    bitmap: Bitmap
): Boolean {
    val resolver = context.contentResolver
    val timestamp = System.currentTimeMillis()
    val fileName = "concafe_checkin_qr_$timestamp.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/ConCafe"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return runCatching {
        resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } == true
    }.onSuccess { success ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pendingValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, pendingValues, null, null)
        }
        if (!success) {
            resolver.delete(uri, null, null)
        }
    }.onFailure {
        resolver.delete(uri, null, null)
    }.getOrDefault(false)
}

private const val QR_SIZE_PX = 1024
private const val BLACK = -0x1000000
private const val WHITE = -0x1
