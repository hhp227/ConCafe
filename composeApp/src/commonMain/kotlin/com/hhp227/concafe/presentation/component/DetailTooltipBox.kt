package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
fun DetailTooltipBox(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    Box(modifier = modifier) {
        content()
        if (visible) {
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val margin = with(density) { 8.dp.roundToPx() }
                        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
                        val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                        val aboveY = anchorBounds.top - popupContentSize.height - margin
                        val belowY = anchorBounds.bottom + margin
                        val y = if (aboveY >= 0) aboveY else belowY

                        return IntOffset(
                            x = centeredX.coerceIn(0, maxX),
                            y = y.coerceIn(0, maxY)
                        )
                    }
                },
                properties = PopupProperties(focusable = false),
                onDismissRequest = {}
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
