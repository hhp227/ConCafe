package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.ContentLayout
import com.hhp227.concafe.domain.repository.UserPreferenceRepository

class SetContentLayoutUseCase(
    private val repository: UserPreferenceRepository
) {
    operator fun invoke(contentLayout: ContentLayout) {
        repository.setContentLayout(contentLayout)
    }
}
