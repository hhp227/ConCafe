package com.hhp227.concafe.presentation.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.hhp227.concafe.data.repository.ScreenCaptureProtectionRepositoryImpl
import com.hhp227.concafe.data.source.createPlatformScreenCaptureProtectionDataSource
import com.hhp227.concafe.domain.usecase.DisableScreenCaptureProtectionUseCase
import com.hhp227.concafe.domain.usecase.EnableScreenCaptureProtectionUseCase

@Composable
fun ScreenCaptureProtectionEffect() {
    val repository = remember {
        ScreenCaptureProtectionRepositoryImpl(createPlatformScreenCaptureProtectionDataSource())
    }
    val enableScreenCaptureProtectionUseCase = remember(repository) {
        EnableScreenCaptureProtectionUseCase(repository)
    }
    val disableScreenCaptureProtectionUseCase = remember(repository) {
        DisableScreenCaptureProtectionUseCase(repository)
    }

    DisposableEffect(Unit) {
        enableScreenCaptureProtectionUseCase()
        onDispose {
            disableScreenCaptureProtectionUseCase()
        }
    }
}
