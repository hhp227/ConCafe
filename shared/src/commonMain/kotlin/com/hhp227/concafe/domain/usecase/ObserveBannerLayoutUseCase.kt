package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.BannerLayout
import com.hhp227.concafe.domain.repository.UserPreferenceRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

class ObserveBannerLayoutUseCase(
    private val repository: UserPreferenceRepository
) {
    @NativeCoroutines
    operator fun invoke(): Flow<BannerLayout> {
        return repository.observeBannerLayout()
    }
}
