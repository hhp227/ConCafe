package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

fun colorFromHex(hex: String): Color {
    val normalized = hex.removePrefix("#")
    val value = normalized.toLongOrNull(16) ?: return Color.Gray
    val argb = (0xFF000000 or value)
    return adaptiveColor(argb)
}

fun adaptiveColor(argb: Long): Color {
    val alpha = ((argb shr 24) and 0xFF).toInt()
    val red = ((argb shr 16) and 0xFF).toInt()
    val green = ((argb shr 8) and 0xFF).toInt()
    val blue = (argb and 0xFF).toInt()

    if (!ConCafeThemeState.isDarkMode) {
        return Color(red = red, green = green, blue = blue, alpha = alpha)
    }

    val adapted = adaptForDarkMode(red / 255f, green / 255f, blue / 255f)
    return Color(
        red = (adapted.first * 255f).toInt().coerceIn(0, 255),
        green = (adapted.second * 255f).toInt().coerceIn(0, 255),
        blue = (adapted.third * 255f).toInt().coerceIn(0, 255),
        alpha = alpha
    )
}

fun setConCafeDarkMode(enabled: Boolean) {
    ConCafeThemeState.isDarkMode = enabled
}

private object ConCafeThemeState {
    var isDarkMode by mutableStateOf(false)
}

private fun adaptForDarkMode(red: Float, green: Float, blue: Float): Triple<Float, Float, Float> {
    val luminance = (0.2126f * red) + (0.7152f * green) + (0.0722f * blue)
    val maxChannel = maxOf(red, green, blue)
    val minChannel = minOf(red, green, blue)
    val saturation = maxChannel - minChannel

    if (saturation < 0.12f) {
        if (luminance > 0.85f) {
            return Triple(0.10f, 0.11f, 0.13f)
        }
        if (luminance > 0.65f) {
            return blend(red, green, blue, target = 0f, ratio = 0.72f)
        }
        if (luminance > 0.45f) {
            return blend(red, green, blue, target = 0f, ratio = 0.52f)
        }
        if (luminance < 0.20f) {
            return blend(red, green, blue, target = 1f, ratio = 0.38f)
        }
        return blend(red, green, blue, target = 1f, ratio = 0.14f)
    }

    if (luminance > 0.80f) {
        return blend(red, green, blue, target = 0f, ratio = 0.65f)
    }
    if (luminance > 0.60f) {
        return blend(red, green, blue, target = 0f, ratio = 0.45f)
    }
    if (luminance < 0.25f) {
        return blend(red, green, blue, target = 1f, ratio = 0.28f)
    }
    return blend(red, green, blue, target = 0f, ratio = 0.18f)
}

private fun blend(red: Float, green: Float, blue: Float, target: Float, ratio: Float): Triple<Float, Float, Float> {
    val clampedRatio = ratio.coerceIn(0f, 1f)
    return Triple(
        (red * (1f - clampedRatio)) + (target * clampedRatio),
        (green * (1f - clampedRatio)) + (target * clampedRatio),
        (blue * (1f - clampedRatio)) + (target * clampedRatio)
    )
}
