package com.hhp227.concafe.presentation.main.cafemanagement

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
actual fun CafeManagementQrCode(
    payload: String,
    modifier: Modifier
) {
    val qrImage = remember(payload) {
        runCatching { createQrImageBitmap(payload) }.getOrNull()
    }
    qrImage?.let { image ->
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

private fun createQrImageBitmap(payload: String): ImageBitmap {
    val bitMatrix = MultiFormatWriter().encode(
        payload,
        BarcodeFormat.QR_CODE,
        QR_SIZE_PX,
        QR_SIZE_PX
    )
    val bufferedImage = BufferedImage(QR_SIZE_PX, QR_SIZE_PX, BufferedImage.TYPE_INT_ARGB)

    for (x in 0 until QR_SIZE_PX) {
        for (y in 0 until QR_SIZE_PX) {
            bufferedImage.setRGB(x, y, if (bitMatrix[x, y]) BLACK else WHITE)
        }
    }
    val bytes = ByteArrayOutputStream().use { output ->
        ImageIO.write(bufferedImage, "png", output)
        output.toByteArray()
    }
    return Image.makeFromEncoded(bytes).asImageBitmap()
}

private const val QR_SIZE_PX = 1024
private const val BLACK = -0x1000000
private const val WHITE = -0x1
