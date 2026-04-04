package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.data.model.NativeAdHandle

class ClearNativeAdUseCase {
    operator fun invoke(ad: NativeAdHandle?) {
        ad?.destroy()
    }
}