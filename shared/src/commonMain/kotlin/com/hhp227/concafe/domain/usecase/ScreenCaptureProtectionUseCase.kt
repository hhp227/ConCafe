package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.repository.ScreenCaptureProtectionRepository

class EnableScreenCaptureProtectionUseCase(
    private val repository: ScreenCaptureProtectionRepository
) {
    operator fun invoke() {
        repository.enable()
    }
}

class DisableScreenCaptureProtectionUseCase(
    private val repository: ScreenCaptureProtectionRepository
) {
    operator fun invoke() {
        repository.disable()
    }
}
