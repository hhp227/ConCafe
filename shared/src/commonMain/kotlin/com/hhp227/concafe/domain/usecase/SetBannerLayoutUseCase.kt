package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.model.BannerLayout
import com.hhp227.concafe.domain.repository.UserPreferenceRepository

class SetBannerLayoutUseCase(
    private val repository: UserPreferenceRepository
) {
    operator fun invoke(bannerLayout: BannerLayout) {
        repository.setBannerLayout(bannerLayout)
    }
}
