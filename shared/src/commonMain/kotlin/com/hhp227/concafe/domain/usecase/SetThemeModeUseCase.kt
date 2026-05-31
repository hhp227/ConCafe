package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.ThemeMode
import com.hhp227.concafe.domain.repository.UserPreferenceRepository

class SetThemeModeUseCase(
    private val repository: UserPreferenceRepository
) {
    operator fun invoke(themeMode: ThemeMode) {
        repository.setThemeMode(themeMode)
    }
}
