package com.hhp227.concafe.presentation.theme

import com.hhp227.concafe.domain.model.ThemeMode

enum class AppThemeMode {
    LIGHT,
    DARK
}

fun ThemeMode.toPresentationThemeMode(): AppThemeMode {
    return when (this) {
        ThemeMode.LIGHT -> AppThemeMode.LIGHT
        ThemeMode.DARK -> AppThemeMode.DARK
    }
}

fun AppThemeMode.toDomainThemeMode(): ThemeMode {
    return when (this) {
        AppThemeMode.LIGHT -> ThemeMode.LIGHT
        AppThemeMode.DARK -> ThemeMode.DARK
    }
}
