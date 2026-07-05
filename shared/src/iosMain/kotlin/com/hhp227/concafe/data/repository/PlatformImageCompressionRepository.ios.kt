package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.CompressedImageData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.sqrt

@OptIn(ExperimentalForeignApi::class)
actual fun compressImageForUpload(localPath: String, maxBytes: Int): CompressedImageData {
    val resolvedPath = resolveLocalPath(localPath)
    val sourceData = NSData.dataWithContentsOfFile(resolvedPath)
        ?: throw IllegalArgumentException("image file not found: $resolvedPath")
    val fileName = resolvedPath.substringAfterLast('/').ifBlank { "image.jpg" }

    if (sourceData.length.toLong() <= maxBytes.toLong()) {
        return CompressedImageData(bytes = sourceData.toByteArray(), fileName = fileName)
    }
    val sourceImage = UIImage.imageWithData(sourceData)
        ?: throw IllegalArgumentException("invalid image file: $resolvedPath")

    var currentImage = downsampleIfNeeded(sourceImage)
    var quality = 0.9
    var bestBytes: NSData? = null
    var attempt = 0

    while (attempt < MAX_COMPRESS_ATTEMPTS) {
        val compressed = UIImageJPEGRepresentation(currentImage, quality) ?: break

        if (bestBytes == null || compressed.length < bestBytes.length) {
            bestBytes = compressed
        }
        if (compressed.length.toLong() < maxBytes.toLong()) {
            return CompressedImageData(bytes = compressed.toByteArray(), fileName = ensureJpegName(fileName))
        }

        val currentWidth = currentImage.pixelWidth()
        val currentHeight = currentImage.pixelHeight()

        if (quality > 0.55) {
            quality -= 0.05
        } else if (currentWidth > MIN_DIMENSION_PX || currentHeight > MIN_DIMENSION_PX) {
            val nextWidth = max(currentWidth * 0.7, MIN_DIMENSION_PX)
            val nextHeight = max(currentHeight * 0.7, MIN_DIMENSION_PX)

            currentImage = resizeImage(currentImage, nextWidth, nextHeight)
            quality = 0.8
        } else {
            break
        }
        attempt += 1
    }
    // 압축 목표에 도달하지 못해도 업로드 자체가 실패하지 않도록 최선 결과를 반환한다
    val fallback = bestBytes ?: sourceData
    return CompressedImageData(bytes = fallback.toByteArray(), fileName = ensureJpegName(fileName))
}

@OptIn(ExperimentalForeignApi::class)
private fun downsampleIfNeeded(image: UIImage): UIImage {
    val width = image.pixelWidth()
    val height = image.pixelHeight()

    return if (width * height > MAX_DECODE_PIXELS) {
        val scaleFactor = sqrt(MAX_DECODE_PIXELS / (width * height))
        resizeImage(
            image = image,
            width = max(width * scaleFactor, MIN_DIMENSION_PX),
            height = max(height * scaleFactor, MIN_DIMENSION_PX)
        )
    } else {
        image
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun resizeImage(image: UIImage, width: Double, height: Double): UIImage {
    val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width, height), format = format)
    return renderer.imageWithActions {
        image.drawInRect(CGRectMake(0.0, 0.0, width, height))
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.pixelWidth(): Double {
    return size.useContents { width } * scale
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.pixelHeight(): Double {
    return size.useContents { height } * scale
}

private fun resolveLocalPath(path: String): String {
    if (path.startsWith("file://")) {
        return NSURL.URLWithString(path)?.path ?: path.removePrefix("file://")
    }
    return path
}

private fun ensureJpegName(fileName: String): String {
    return if (fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true)) {
        fileName
    } else {
        fileName.substringBeforeLast('.') + ".jpg"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val byteArray = ByteArray(size)
    memScoped {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}

private const val MAX_COMPRESS_ATTEMPTS = 20
private const val MAX_DECODE_PIXELS = 4_000_000.0
private const val MIN_DIMENSION_PX = 480.0
