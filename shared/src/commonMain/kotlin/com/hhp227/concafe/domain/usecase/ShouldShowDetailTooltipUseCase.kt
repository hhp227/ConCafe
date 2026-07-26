package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.DetailTooltipType
import com.hhp227.concafe.domain.repository.UserPreferenceRepository

class ShouldShowDetailTooltipUseCase(
    private val repository: UserPreferenceRepository
) {
    operator fun invoke(type: DetailTooltipType): Boolean {
        return !repository.hasShownDetailTooltip(type)
    }
}
