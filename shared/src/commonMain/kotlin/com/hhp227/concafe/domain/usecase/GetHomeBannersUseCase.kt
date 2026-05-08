package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.repository.BannerRepository

class GetHomeBannersUseCase(
    private val bannerRepository: BannerRepository
) {
    suspend operator fun invoke(limit: Int = HOME_BANNER_LIMIT): AppResult<List<HomeBanner>> {
        return try {
            AppResult.Success(bannerRepository.getHomeBanners(limit.coerceAtLeast(1)))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }

    companion object {
        private const val HOME_BANNER_LIMIT = 6
    }
}
