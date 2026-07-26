package com.hhp227.concafe.presentation.picture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ImageDisplaySize

@Composable
fun PictureScreen(
    imageUrl: String,
    onAction: (PictureAction) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CompatImageDisplay(
            imageUrl = imageUrl,
            modifier = Modifier.fillMaxSize(),
            applyRoundedClip = false,
            contentScale = ContentScale.Fit,
            displaySize = ImageDisplaySize.FULL
        )
        IconButton(
            onClick = { onAction(PictureAction.ClickBack) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
