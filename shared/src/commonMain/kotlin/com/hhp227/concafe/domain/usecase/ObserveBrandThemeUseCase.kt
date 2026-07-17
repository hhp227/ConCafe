package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.repository.UserPreferenceRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

class ObserveBrandThemeUseCase(
    private val repository: UserPreferenceRepository
) {
    @NativeCoroutines
    operator fun invoke(): Flow<BrandTheme> {
        return repository.observeBrandTheme()
    }
}
