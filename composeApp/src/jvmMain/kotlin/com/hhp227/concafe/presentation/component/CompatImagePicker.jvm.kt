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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.GraphicsEnvironment
import java.io.File
import java.net.URL
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun CompatImagePicker(
    onImageSelected: (String) -> Unit,
    content: @Composable (launchPicker: () -> Unit) -> Unit
) {
    val launchPicker = {
        chooseImageFile()?.let(onImageSelected)
    }

    content(launchPicker)
}

@Composable
actual fun CompatImageDisplay(
    imageUrl: String?,
    modifier: Modifier,
    applyRoundedClip: Boolean
) {
    val normalizedImageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = normalizedImageUrl) {
        value = if (normalizedImageUrl == null) null else decodeImageBitmap(normalizedImageUrl)
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
                contentScale = ContentScale.Crop
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

private suspend fun decodeImageBitmap(imageUrl: String): ImageBitmap? {
    JvmImageBitmapMemoryCache.get(imageUrl)?.let { cached ->
        return cached
    }
    return withContext(Dispatchers.IO) {
        runCatching {
            val bytes = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                URL(imageUrl).readBytes()
            } else {
                File(imageUrl).readBytes()
            }
            val decoded = Image.makeFromEncoded(bytes).asImageBitmap()

            JvmImageBitmapMemoryCache.put(imageUrl, decoded)
            decoded
        }.getOrNull()
    }
}

private object JvmImageBitmapMemoryCache {
    private const val MAX_ENTRIES = 120
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
