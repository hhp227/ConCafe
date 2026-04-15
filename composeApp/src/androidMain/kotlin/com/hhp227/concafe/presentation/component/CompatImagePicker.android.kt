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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
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
    val coilSize = when (displaySize) {
        ImageDisplaySize.THUMBNAIL -> Size(640, 640)
        ImageDisplaySize.MEDIUM -> Size(1200, 1200)
        ImageDisplaySize.FULL -> Size.ORIGINAL
    }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .size(coilSize)
            .crossfade(true)
            .build()
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
