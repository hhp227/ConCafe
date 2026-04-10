package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

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
    contentScale: ContentScale = ContentScale.Crop
)
