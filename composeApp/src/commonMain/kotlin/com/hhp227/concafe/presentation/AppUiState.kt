package com.hhp227.concafe.presentation

import com.hhp227.concafe.domain.model.NetworkAlertState
import com.hhp227.concafe.presentation.theme.AppThemeMode

data class AppUiState(
    val networkAlertState: NetworkAlertState = NetworkAlertState.hidden,
    val hasUnreadNotifications: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT
)
