package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import androidx.compose.runtime.Composable

@Composable
expect fun rememberCafeManagementQrCodeSaver(): suspend (String) -> Boolean
