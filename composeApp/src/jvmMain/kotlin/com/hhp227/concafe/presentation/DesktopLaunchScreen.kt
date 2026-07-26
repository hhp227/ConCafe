package com.hhp227.concafe.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.splash_logo
import org.jetbrains.compose.resources.painterResource
import com.hhp227.concafe.presentation.component.ConCafeColors

@Composable
fun DesktopLaunchScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ConCafeColors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
