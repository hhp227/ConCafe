package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.BrandTheme
import com.hhp227.concafe.domain.repository.UserPreferenceRepository

class SetBrandThemeUseCase(
    private val repository: UserPreferenceRepository
) {
    operator fun invoke(brandTheme: BrandTheme) {
        repository.setBrandTheme(brandTheme)
    }
}
