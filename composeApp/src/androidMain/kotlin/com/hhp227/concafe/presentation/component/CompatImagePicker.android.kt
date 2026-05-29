package com.hhp227.concafe.presentation.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import okio.Path.Companion.toOkioPath
import java.io.File

@Composable
actual fun CompatImagePicker(
    onImageSelected: (String) -> Unit,
    content: @Composable (launchPicker: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                saveToCacheFile(context, it)?.let(onImageSelected)
            }
        }
    )

    content {
        imagePicker.launch("image/*")
    }
}

private fun saveToCacheFile(context: Context, uri: Uri): String? {
    return runCatching {
        val fileName = "img-${System.currentTimeMillis()}.jpg"
        val outputFile = File(context.cacheDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        outputFile.absolutePath
    }.getOrNull()
}

@Composable
actual fun CompatImageDisplay(
    imageUrl: String?,
    modifier: Modifier,
    applyRoundedClip: Boolean,
    contentScale: ContentScale,
    displaySize: ImageDisplaySize
) {
    val shape = if (applyRoundedClip) RoundedCornerShape(20.dp) else null
    val context = LocalContext.current
    val imageLoader = remember(context) { AndroidAppImageLoader.get(context) }
    val normalizedImageUrl = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
    val coilSize = displaySize.coilSize()
    val cacheKey = normalizedImageUrl?.let { stableImageCacheKey(it, displaySize) }
    val request = remember(normalizedImageUrl, displaySize, cacheKey) {
        ImageRequest.Builder(context)
            .data(normalizedImageUrl)
            .size(coilSize)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
    val painter = rememberAsyncImagePainter(
        model = request,
        imageLoader = imageLoader
    )

    Box(modifier = if (shape != null) modifier.clip(shape) else modifier) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale
        )
        if (painter.state is AsyncImagePainter.State.Loading || painter.state is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x19000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
actual fun rememberImagePrefetcher(): ImagePrefetcher {
    val context = LocalContext.current
    val imageLoader = remember(context) { AndroidAppImageLoader.get(context) }
    return remember(imageLoader, context) {
        object : ImagePrefetcher {
            override fun prefetch(imageUrls: List<String?>, displaySize: ImageDisplaySize) {
                imageUrls
                    .asSequence()
                    .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
                    .distinct()
                    .forEach { url ->
                        val cacheKey = stableImageCacheKey(url, displaySize)
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .size(displaySize.coilSize())
                            .memoryCacheKey(cacheKey)
                            .diskCacheKey(cacheKey)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build()
                        imageLoader.enqueue(request)
                    }
            }
        }
    }
}

private object AndroidAppImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.22)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.applicationContext.cacheDir, "image_cache").toOkioPath())
                        .maxSizeBytes(300L * 1024L * 1024L)
                        .build()
                }
                .crossfade(true)
                .respectCacheHeaders(false)
                .build()
                .also { instance = it }
        }
    }
}

private fun ImageDisplaySize.coilSize(): Size = when (this) {
    ImageDisplaySize.THUMBNAIL -> Size(512, 512)
    ImageDisplaySize.MEDIUM -> Size(1200, 1200)
    ImageDisplaySize.FULL -> Size(3840, 3840)
}

private fun stableImageCacheKey(imageUrl: String, displaySize: ImageDisplaySize): String {
    return "${imageUrl.trim()}|${displaySize.name}"
}
