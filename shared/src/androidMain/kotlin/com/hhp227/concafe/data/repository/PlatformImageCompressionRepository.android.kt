package com.hhp227.concafe.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hhp227.concafe.domain.repository.CompressedImageData
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

actual fun compressImageForUpload(localPath: String, maxBytes: Int): CompressedImageData {
    val path = normalizeFilePath(localPath)
    val sourceFile = File(path)
    require(sourceFile.exists()) { "image file not found: $path" }

    val originalBytes = sourceFile.readBytes()
    val fileName = sourceFile.name.ifBlank { "image.jpg" }
    if (originalBytes.size <= maxBytes) {
        return CompressedImageData(bytes = originalBytes, fileName = fileName)
    }

    val originalBitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        ?: throw IllegalArgumentException("invalid image file: $path")

    var currentBitmap = originalBitmap
    var quality = 90
    var attempt = 0

    while (attempt < 12) {
        val compressed = currentBitmap.compressJpeg(quality)
        if (compressed.size < maxBytes) {
            if (currentBitmap !== originalBitmap) currentBitmap.recycle()
            originalBitmap.recycle()
            return CompressedImageData(bytes = compressed, fileName = ensureJpegName(fileName))
        }

        if (quality > 55) {
            quality -= 5
        } else {
            val nextWidth = max((currentBitmap.width * 0.85f).toInt(), 480)
            val nextHeight = max((currentBitmap.height * 0.85f).toInt(), 480)
            val resized = Bitmap.createScaledBitmap(currentBitmap, nextWidth, nextHeight, true)
            if (currentBitmap !== originalBitmap) {
                currentBitmap.recycle()
            }
            currentBitmap = resized
            quality = 85
        }
        attempt += 1
    }

    if (currentBitmap !== originalBitmap) currentBitmap.recycle()
    originalBitmap.recycle()
    throw IllegalArgumentException("failed to compress image under ${maxBytes}bytes")
}

private fun Bitmap.compressJpeg(quality: Int): ByteArray {
    val stream = ByteArrayOutputStream()
    val success = compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), stream)
    check(success) { "bitmap compression failed" }
    return stream.toByteArray()
}

private fun normalizeFilePath(path: String): String {
    return path.removePrefix("file://")
}

private fun ensureJpegName(fileName: String): String {
    return if (fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true)) {
        fileName
    } else {
        fileName.substringBeforeLast('.') + ".jpg"
    }
}
