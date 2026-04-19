package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.data.model.NativeAdHandle
import com.hhp227.concafe.domain.repository.NativeAdRepository

class LoadNativeAdUseCase(
    private val repository: NativeAdRepository
) {
    suspend operator fun invoke(slot: Int): NativeAdHandle? {
        return repository.loadAd(slot)
    }
}
