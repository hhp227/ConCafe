package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt

@Composable
actual fun CompatImagePicker(
    onImageSelected: (String) -> Unit,
    content: @Composable (launchPicker: () -> Unit) -> Unit
) {
    val launchPicker = {
        chooseImageFile()?.let(onImageSelected)
        Unit
    }

    content(launchPicker)
}

@Composable
actual fun CompatImageDisplay(
    imageUrl: String?,
    modifier: Modifier,
    applyRoundedClip: Boolean,
    contentScale: ContentScale,
    displaySize: ImageDisplaySize
) {
    val normalizedImageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
    // Cache key includes displaySize so the same URL can be cached at different resolutions.
    val cacheKey = if (normalizedImageUrl == null) null else "$normalizedImageUrl|$displaySize"
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = cacheKey) {
        value = if (cacheKey == null || normalizedImageUrl == null) null
        else decodeImageBitmap(normalizedImageUrl, cacheKey, displaySize)
    }
    val resolvedImageBitmap = imageBitmap

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (resolvedImageBitmap == null) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.Image),
                contentDescription = null,
                tint = Color(0xFF8C7A85),
                modifier = Modifier.fillMaxSize(0.36f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color(0x1A8B6F7A),
                        if (applyRoundedClip) RoundedCornerShape(20.dp) else RoundedCornerShape(0.dp)
                    )
            )
        } else {
            val imageModifier = if (applyRoundedClip) {
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
            } else {
                Modifier.fillMaxSize()
            }
            Image(
                bitmap = resolvedImageBitmap,
                contentDescription = null,
                modifier = imageModifier,
                contentScale = contentScale
            )
        }
    }
}

private fun chooseImageFile(): String? {
    if (!GraphicsEnvironment.isHeadless()) {
        val chooser = JFileChooser().apply {
            dialogTitle = "이미지 선택"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isAcceptAllFileFilterUsed = false
            fileFilter = FileNameExtensionFilter(
                "이미지 파일 (JPG, JPEG, PNG, WEBP)",
                "jpg",
                "jpeg",
                "png",
                "webp"
            )
        }
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile?.let { File(it.absolutePath).absolutePath }
        } else {
            null
        }
    } else {
        return null
    }
}

private suspend fun decodeImageBitmap(
    imageUrl: String,
    cacheKey: String,
    displaySize: ImageDisplaySize
): ImageBitmap? {
    JvmImageBitmapMemoryCache.get(cacheKey)?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            val original: BufferedImage? = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                val connection = URL(imageUrl).openConnection().apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
                connection.getInputStream().use { ImageIO.read(it) }
            } else {
                File(imageUrl).inputStream().use { ImageIO.read(it) }
            }
            val source = original ?: return@runCatching null
            val maxPx = displaySize.maxPx()
            val scaled = if (maxPx != null) scaleDown(source, maxPx) else source
            val bitmap = scaled.toComposeImageBitmap()

            JvmImageBitmapMemoryCache.put(cacheKey, bitmap)
            bitmap
        }.getOrNull()
    }
}

/** Returns null for FULL (no downscaling). */
private fun ImageDisplaySize.maxPx(): Int? = when (this) {
    ImageDisplaySize.THUMBNAIL -> 512
    ImageDisplaySize.MEDIUM -> 1200
    ImageDisplaySize.FULL -> null
}

private fun scaleDown(source: BufferedImage, maxPx: Int): BufferedImage {
    val srcW = source.width
    val srcH = source.height
    if (srcW <= maxPx && srcH <= maxPx) return source
    val ratio = maxPx.toFloat() / maxOf(srcW, srcH)
    val dstW = (srcW * ratio).roundToInt().coerceAtLeast(1)
    val dstH = (srcH * ratio).roundToInt().coerceAtLeast(1)
    val type = if (source.type != BufferedImage.TYPE_CUSTOM) source.type else BufferedImage.TYPE_INT_ARGB
    val result = BufferedImage(dstW, dstH, type)
    val g = result.createGraphics()

    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(source, 0, 0, dstW, dstH, null)
    g.dispose()
    return result
}

private object JvmImageBitmapMemoryCache {
    private const val MAX_ENTRIES = 200
    private val cache = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    fun get(key: String): ImageBitmap? = synchronized(cache) {
        cache[key]
    }

    fun put(key: String, value: ImageBitmap) = synchronized(cache) {
        cache[key] = value
    }
}
