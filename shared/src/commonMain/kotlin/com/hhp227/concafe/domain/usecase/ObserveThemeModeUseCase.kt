package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.ThemeMode
import com.hhp227.concafe.domain.repository.UserPreferenceRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(
    private val repository: UserPreferenceRepository
) {
    @NativeCoroutines
    operator fun invoke(): Flow<ThemeMode> {
        return repository.observeThemeMode()
    }
}
