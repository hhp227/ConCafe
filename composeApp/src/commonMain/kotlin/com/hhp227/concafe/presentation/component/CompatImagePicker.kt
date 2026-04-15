package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

enum class ImageDisplaySize {
    /** Small list cards, thumbnails (~512px max dimension) */
    THUMBNAIL,
    /** Banners, event cards, medium-sized display (~1200px max dimension) */
    MEDIUM,
    /** Full-screen picture viewer — no downscaling */
    FULL
}

@Composable
expect fun CompatImagePicker(
    onImageSelected: (String) -> Unit,
    content: @Composable (launchPicker: () -> Unit) -> Unit
)

@Composable
expect fun CompatImageDisplay(
    imageUrl: String?,
    modifier: Modifier,
    applyRoundedClip: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
    displaySize: ImageDisplaySize = ImageDisplaySize.THUMBNAIL
)
