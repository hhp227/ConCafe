package com.hhp227.concafe.data.repository

import com.hhp227.concafe.domain.model.CompressedImageData
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.max

actual fun compressImageForUpload(localPath: String, maxBytes: Int): CompressedImageData {
    val path = localPath.removePrefix("file://")
    val sourceFile = File(path)
    require(sourceFile.exists()) { "image file not found: $path" }

    val originalBytes = sourceFile.readBytes()
    val fileName = sourceFile.name.ifBlank { "image.jpg" }
    if (originalBytes.size <= maxBytes) {
        return CompressedImageData(bytes = originalBytes, fileName = fileName)
    }

    val loadedImage = ImageIO.read(sourceFile) ?: throw IllegalArgumentException("invalid image file: $path")
    var currentImage = downsampleIfNeeded(if (loadedImage.type == BufferedImage.TYPE_INT_RGB) loadedImage else convertToRgb(loadedImage))
    var quality = 0.9f
    var bestBytes: ByteArray? = null
    var attempt = 0

    while (attempt < MAX_COMPRESS_ATTEMPTS) {
        val compressed = encodeJpeg(currentImage, quality)

        if (bestBytes == null || compressed.size < bestBytes.size) {
            bestBytes = compressed
        }
        if (compressed.size < maxBytes) {
            return CompressedImageData(bytes = compressed, fileName = ensureJpegName(fileName))
        }

        if (quality > 0.55f) {
            quality -= 0.05f
        } else if (currentImage.width > MIN_DIMENSION_PX || currentImage.height > MIN_DIMENSION_PX) {
            val nextWidth = max((currentImage.width * 0.7f).toInt(), MIN_DIMENSION_PX)
            val nextHeight = max((currentImage.height * 0.7f).toInt(), MIN_DIMENSION_PX)
            currentImage = resizeImage(currentImage, nextWidth, nextHeight)
            quality = 0.8f
        } else {
            break
        }
        attempt += 1
    }

    // 압축 목표에 도달하지 못해도 업로드 자체가 실패하지 않도록 최선 결과를 반환한다
    return CompressedImageData(bytes = bestBytes ?: originalBytes, fileName = ensureJpegName(fileName))
}

private fun downsampleIfNeeded(source: BufferedImage): BufferedImage {
    val pixelCount = source.width.toLong() * source.height

    return if (pixelCount > MAX_DECODE_PIXELS) {
        val scaleFactor = kotlin.math.sqrt(MAX_DECODE_PIXELS.toDouble() / pixelCount)
        resizeImage(
            source = source,
            width = max((source.width * scaleFactor).toInt(), MIN_DIMENSION_PX),
            height = max((source.height * scaleFactor).toInt(), MIN_DIMENSION_PX)
        )
    } else {
        source
    }
}

private fun convertToRgb(source: BufferedImage): BufferedImage {
    val converted = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
    val graphics = converted.createGraphics()
    graphics.drawImage(source, 0, 0, null)
    graphics.dispose()
    return converted
}

private fun resizeImage(source: BufferedImage, width: Int, height: Int): BufferedImage {
    val scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH)
    val resized = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics: Graphics2D = resized.createGraphics()
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    graphics.drawImage(scaled, 0, 0, null)
    graphics.dispose()
    return resized
}

private fun encodeJpeg(image: BufferedImage, quality: Float): ByteArray {
    val writer = ImageIO.getImageWritersByFormatName("jpg").asSequence().firstOrNull()
        ?: throw IllegalStateException("jpeg writer is not available")
    val output = ByteArrayOutputStream()
    val ios = ImageIO.createImageOutputStream(output)
    writer.output = ios
    val params: ImageWriteParam = writer.defaultWriteParam.apply {
        compressionMode = ImageWriteParam.MODE_EXPLICIT
        compressionQuality = quality.coerceIn(0.1f, 1.0f)
    }
    writer.write(null, IIOImage(image, null, null), params)
    ios.close()
    writer.dispose()
    return output.toByteArray()
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
