package com.hhp227.concafe.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.NetworkAlertState

@Composable
fun NetworkStatusBanner(
    networkAlertState: NetworkAlertState,
    modifier: Modifier = Modifier
) {
    val containerColor = if (networkAlertState.isConnected) {
        colorFromHex("DCFCE7")
    } else {
        colorFromHex("FEE2E2")
    }
    val contentColor = if (networkAlertState.isConnected) {
        colorFromHex("166534")
    } else {
        colorFromHex("991B1B")
    }

    AnimatedVisibility(
        visible = networkAlertState.isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.statusBarsPadding()
    ) {
        Surface(
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = networkAlertState.message,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
