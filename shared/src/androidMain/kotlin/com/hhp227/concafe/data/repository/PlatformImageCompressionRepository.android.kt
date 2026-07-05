package com.hhp227.concafe.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.hhp227.concafe.domain.model.CompressedImageData
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

actual fun compressImageForUpload(localPath: String, maxBytes: Int): CompressedImageData {
    val path = normalizeFilePath(localPath)
    val sourceFile = File(path)
    require(sourceFile.exists()) { "image file not found: $path" }

    val fileName = sourceFile.name.ifBlank { "image.jpg" }
    if (sourceFile.length() <= maxBytes) {
        return CompressedImageData(bytes = sourceFile.readBytes(), fileName = fileName)
    }

    val decodedBitmap = decodeDownsampledBitmap(path)
        ?: throw IllegalArgumentException("invalid image file: $path")
    val originalBitmap = applyExifOrientation(path, decodedBitmap)

    var currentBitmap = originalBitmap
    var quality = 90
    var bestBytes: ByteArray? = null
    var attempt = 0

    while (attempt < MAX_COMPRESS_ATTEMPTS) {
        val compressed = currentBitmap.compressJpeg(quality)

        if (bestBytes == null || compressed.size < bestBytes.size) {
            bestBytes = compressed
        }
        if (compressed.size < maxBytes) {
            if (currentBitmap !== originalBitmap) currentBitmap.recycle()
            originalBitmap.recycle()
            return CompressedImageData(bytes = compressed, fileName = ensureJpegName(fileName))
        }

        if (quality > 55) {
            quality -= 5
        } else if (currentBitmap.width > MIN_DIMENSION_PX || currentBitmap.height > MIN_DIMENSION_PX) {
            val nextWidth = max((currentBitmap.width * 0.7f).toInt(), MIN_DIMENSION_PX)
            val nextHeight = max((currentBitmap.height * 0.7f).toInt(), MIN_DIMENSION_PX)
            val resized = Bitmap.createScaledBitmap(currentBitmap, nextWidth, nextHeight, true)
            if (currentBitmap !== originalBitmap) {
                currentBitmap.recycle()
            }
            currentBitmap = resized
            quality = 80
        } else {
            break
        }
        attempt += 1
    }

    if (currentBitmap !== originalBitmap) currentBitmap.recycle()
    originalBitmap.recycle()
    // 압축 목표에 도달하지 못해도 업로드 자체가 실패하지 않도록 최선 결과를 반환한다
    return CompressedImageData(bytes = bestBytes ?: sourceFile.readBytes(), fileName = ensureJpegName(fileName))
}

private fun decodeDownsampledBitmap(path: String): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }

    BitmapFactory.decodeFile(path, boundsOptions)
    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
        return null
    }
    var sampleSize = 1

    while (boundsOptions.outWidth.toLong() * boundsOptions.outHeight / (sampleSize.toLong() * sampleSize) > MAX_DECODE_PIXELS) {
        sampleSize *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeFile(path, decodeOptions)
}

private fun applyExifOrientation(path: String, bitmap: Bitmap): Bitmap {
    val orientation = try {
        ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val matrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return bitmap
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    if (rotated !== bitmap) {
        bitmap.recycle()
    }
    return rotated
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

private const val MAX_COMPRESS_ATTEMPTS = 20
private const val MAX_DECODE_PIXELS = 4_000_000L
private const val MIN_DIMENSION_PX = 480
