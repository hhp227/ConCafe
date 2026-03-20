package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.BannerEvent
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.repository.CafeManagementRepository

class DeleteHomeBannerUseCase(
    private val authRepository: AuthRepository,
    private val cafeManagementRepository: CafeManagementRepository,
    private val bannerRepository: BannerRepository,
    private val bannerEventPublisher: BannerEventPublisher
) {
    suspend operator fun invoke(bannerId: String): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.CAFE_OWNER && currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                val allBanners = bannerRepository.getAllHomeBanners()
                val targetBanner = allBanners.firstOrNull { it.id == bannerId }
                    ?: return AppResult.Failure(AppError.NotFound)
                if (currentUser.role == UserRole.CAFE_OWNER) {
                    val ownedCafeIds = cafeManagementRepository
                        .getCafeManagementData(currentUser.id)
                        .ownedCafes
                        .map { it.id }
                        .toSet()
                    if (targetBanner.cafeId.isNullOrBlank() || !ownedCafeIds.contains(targetBanner.cafeId)) {
                        return AppResult.Failure(AppError.PermissionDenied)
                    }
                }

                val deleted = bannerRepository.deleteHomeBanner(bannerId)

                bannerEventPublisher.publish(BannerEvent.Deleted(deleted))
                AppResult.Success(Unit)
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
