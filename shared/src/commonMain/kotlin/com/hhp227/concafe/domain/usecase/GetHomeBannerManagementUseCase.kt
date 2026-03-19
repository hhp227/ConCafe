package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.BannerRepository

class GetHomeBannerManagementUseCase(
    private val authRepository: AuthRepository,
    private val bannerRepository: BannerRepository
) {
    suspend operator fun invoke(cafeId: String?): AppResult<List<HomeBanner>> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.CAFE_OWNER) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                val allBanners = bannerRepository.getAllHomeBanners()
                val filtered = when (currentUser.role) {
                    UserRole.ADMIN -> {
                        if (cafeId.isNullOrBlank()) {
                            allBanners
                        } else {
                            allBanners.filter { it.cafeId == cafeId }
                        }
                    }
                    UserRole.CAFE_OWNER -> {
                        if (cafeId.isNullOrBlank()) {
                            return AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
                        }
                        allBanners.filter { it.cafeId == cafeId }
                    }
                    else -> emptyList()
                }

                AppResult.Success(filtered)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
