package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.repository.NativeAdRepository

class ClearNativeAdUseCase(
    private val repository: NativeAdRepository
) {
    operator fun invoke() {
        repository.clear()
    }
}