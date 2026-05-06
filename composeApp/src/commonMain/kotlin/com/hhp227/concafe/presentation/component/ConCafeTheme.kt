package com.hhp227.concafe.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun ConCafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val darkMode = darkTheme
    val colorScheme = if (darkMode) {
        darkColorScheme(
            primary = colorFromHex("EF6797"),
            secondary = colorFromHex("F7A0C1"),
            tertiary = colorFromHex("FFD1DC"),
            background = colorFromHex("111317"),
            surface = colorFromHex("171A20"),
            onSurface = colorFromHex("F3EFF2"),
            onBackground = colorFromHex("F3EFF2")
        )
    } else {
        lightColorScheme(
            primary = colorFromHex("EF6797"),
            secondary = colorFromHex("F7A0C1"),
            tertiary = colorFromHex("FFD1DC"),
            background = colorFromHex("FFFBFD"),
            surface = ColorWhite,
            onSurface = colorFromHex("2B2330"),
            onBackground = colorFromHex("2B2330")
        )
    }

    SideEffect { setConCafeDarkMode(darkMode) }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private val ColorWhite = colorFromHex("FFFFFF")
