package com.hhp227.concafe.presentation.main.cafemanagement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberCafeManagementQrCodeSaver(): suspend (String) -> Boolean {
    return remember {
        { _ -> false }
    }
}
