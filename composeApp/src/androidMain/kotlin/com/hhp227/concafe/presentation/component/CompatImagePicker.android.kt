package com.hhp227.concafe.presentation.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.net.URL

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
    applyRoundedClip: Boolean
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUrl) {
        bitmap = imageUrl?.let { url ->
            decodeImageBitmap(context, url)
        }
    }
    if (bitmap == null) {
        val placeholderModifier = if (applyRoundedClip) {
            modifier.clip(RoundedCornerShape(20.dp))
        } else {
            modifier
        }
        Box(
            modifier = placeholderModifier
                .fillMaxWidth()
                .background(Color(0x19000000)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberVectorPainter(Icons.Default.Image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    } else {
        val imageModifier = if (applyRoundedClip) {
            modifier.clip(RoundedCornerShape(20.dp))
        } else {
            modifier
        }
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = imageModifier,
            contentScale = ContentScale.Crop
        )
    }
}

private suspend fun decodeImageBitmap(
    context: Context,
    imageUrl: String
): Bitmap? = withContext(Dispatchers.IO) {
    AndroidBitmapMemoryCache.get(imageUrl)?.let { cached ->
        return@withContext cached
    }

    runCatching {
        val stream = when {
            imageUrl.startsWith("content://") || imageUrl.startsWith("file://") ->
                context.contentResolver.openInputStream(Uri.parse(imageUrl))
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") ->
                URL(imageUrl).openStream()
            else -> FileInputStream(imageUrl)
        }

        val decoded = stream.use { BitmapFactory.decodeStream(it) }
        if (decoded != null) {
            AndroidBitmapMemoryCache.put(imageUrl, decoded)
        }
        decoded
    }.getOrNull()
}

private object AndroidBitmapMemoryCache {
    private val cache = object : LruCache<String, Bitmap>(120) {}

    fun get(key: String): Bitmap? {
        return cache.get(key)
    }

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}
