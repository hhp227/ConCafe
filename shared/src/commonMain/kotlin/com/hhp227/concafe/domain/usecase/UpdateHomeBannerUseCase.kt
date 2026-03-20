package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.BannerEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.model.BannerLinkTargetType
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository

class UpdateHomeBannerUseCase(
    private val authRepository: AuthRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val bannerRepository: BannerRepository,
    private val bannerEventPublisher: BannerEventPublisher
) {
    suspend operator fun invoke(bannerId: String, input: HomeBannerCreate): AppResult<HomeBanner> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.CAFE_OWNER) {
                AppResult.Failure(AppError.PermissionDenied)
            } else if (input.targetType != BannerLinkTargetType.EXTERNAL_LINK && input.cafeId.isNullOrBlank()) {
                AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            } else {
                val existingBanner = bannerRepository
                    .getAllHomeBanners()
                    .firstOrNull { it.id == bannerId }
                    ?: return AppResult.Failure(AppError.NotFound)

                if (currentUser.role == UserRole.CAFE_OWNER) {
                    val ownedCafeIds = cafeManagementRepository.getCafeManagementData(currentUser.id)
                        .ownedCafes
                        .map { it.id }
                        .toSet()
                    if (existingBanner.cafeId.isNullOrBlank() || !ownedCafeIds.contains(existingBanner.cafeId)) {
                        return AppResult.Failure(AppError.PermissionDenied)
                    }
                    if (!input.cafeId.isNullOrBlank() && !ownedCafeIds.contains(input.cafeId)) {
                        return AppResult.Failure(AppError.PermissionDenied)
                    }
                }

                val updated = bannerRepository.updateHomeBanner(
                    bannerId = bannerId,
                    input = input
                )

                bannerEventPublisher.publish(BannerEvent.Updated(updated))
                AppResult.Success(updated)
            }
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
