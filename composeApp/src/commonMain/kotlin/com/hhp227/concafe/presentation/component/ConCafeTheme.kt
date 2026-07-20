package com.hhp227.concafe.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import com.hhp227.concafe.presentation.theme.AppBrandTheme

@Composable
fun ConCafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    brandTheme: AppBrandTheme = AppBrandTheme.MAID_CAFE,
    content: @Composable () -> Unit
) {
    val darkMode = darkTheme
    val colorScheme = if (darkMode) {
        darkColorScheme(
            primary = ConCafeColors.primary,
            onPrimary = ConCafeColors.onPrimary,
            primaryContainer = ConCafeColors.primaryContainer,
            onPrimaryContainer = ConCafeColors.onPrimaryContainer,
            secondary = ConCafeColors.secondary,
            onSecondary = ConCafeColors.onSecondary,
            secondaryContainer = ConCafeColors.secondaryContainer,
            onSecondaryContainer = ConCafeColors.onSecondaryContainer,
            tertiary = ConCafeColors.tertiary,
            onTertiary = ConCafeColors.onTertiary,
            tertiaryContainer = ConCafeColors.tertiaryContainer,
            onTertiaryContainer = ConCafeColors.onTertiaryContainer,
            background = ConCafeColors.background,
            surface = ConCafeColors.surface,
            surfaceVariant = ConCafeColors.surfaceVariant,
            onSurface = ConCafeColors.textPrimary,
            onBackground = ConCafeColors.textPrimary,
            onSurfaceVariant = ConCafeColors.textSecondary,
            outline = ConCafeColors.outline,
            outlineVariant = ConCafeColors.outline,
            error = ConCafeColors.error,
            errorContainer = ConCafeColors.errorContainer
        )
    } else {
        lightColorScheme(
            primary = ConCafeColors.primary,
            onPrimary = ConCafeColors.onPrimary,
            primaryContainer = ConCafeColors.primaryContainer,
            onPrimaryContainer = ConCafeColors.onPrimaryContainer,
            secondary = ConCafeColors.secondary,
            onSecondary = ConCafeColors.onSecondary,
            secondaryContainer = ConCafeColors.secondaryContainer,
            onSecondaryContainer = ConCafeColors.onSecondaryContainer,
            tertiary = ConCafeColors.tertiary,
            onTertiary = ConCafeColors.onTertiary,
            tertiaryContainer = ConCafeColors.tertiaryContainer,
            onTertiaryContainer = ConCafeColors.onTertiaryContainer,
            background = ConCafeColors.background,
            surface = ConCafeColors.surface,
            surfaceVariant = ConCafeColors.surfaceVariant,
            onSurface = ConCafeColors.textPrimary,
            onBackground = ConCafeColors.textPrimary,
            onSurfaceVariant = ConCafeColors.textSecondary,
            outline = ConCafeColors.outline,
            outlineVariant = ConCafeColors.outline,
            error = ConCafeColors.error,
            errorContainer = ConCafeColors.errorContainer
        )
    }

    SideEffect {
        setConCafeDarkMode(darkMode)
        setConCafeBrandTheme(brandTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
