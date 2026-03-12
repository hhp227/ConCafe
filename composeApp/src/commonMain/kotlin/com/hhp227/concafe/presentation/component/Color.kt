package com.hhp227.concafe.presentation.component

import androidx.compose.ui.graphics.Color

fun colorFromHex(hex: String): Color {
    val normalized = hex.removePrefix("#")
    val value = normalized.toLongOrNull(16) ?: return Color.Gray
    return Color(
        red = ((value shr 16) and 0xFF).toInt(),
        green = ((value shr 8) and 0xFF).toInt(),
        blue = (value and 0xFF).toInt()
    )
}