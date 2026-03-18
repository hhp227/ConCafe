package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.HomeBanner
import com.hhp227.concafe.domain.model.HomeBannerCreate
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.BannerRepository
import com.hhp227.concafe.domain.event.publisher.BannerEventPublisher
import com.hhp227.concafe.domain.event.BannerEvent

class CreateHomeBannerUseCase(
    private val authRepository: AuthRepository,
    private val bannerRepository: BannerRepository,
    private val bannerEventPublisher: BannerEventPublisher
) {
    suspend operator fun invoke(input: HomeBannerCreate): AppResult<HomeBanner> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.CAFE_OWNER) {
                return AppResult.Failure(AppError.PermissionDenied)
            }

            if (currentUser.role == UserRole.CAFE_OWNER && input.cafeId.isNullOrBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            }

            if (input.targetType != com.hhp227.concafe.domain.model.BannerLinkTargetType.EXTERNAL_LINK &&
                input.cafeId.isNullOrBlank()
            ) {
                return AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            }
            val banner = bannerRepository.createHomeBanner(input)

            bannerEventPublisher.publish(BannerEvent.Created(banner))
            AppResult.Success(banner)
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
