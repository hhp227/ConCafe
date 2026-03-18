package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CafeRegistrationClaimEvent
import com.hhp227.concafe.domain.event.publisher.CafeRegistrationClaimEventPublisher
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRegistrationClaimRepository

class ApproveCafeRegistrationClaimUseCase(
    private val authRepository: AuthRepository,
    private val cafeRegistrationClaimRepository: CafeRegistrationClaimRepository,
    private val cafeRegistrationClaimEventPublisher: CafeRegistrationClaimEventPublisher
) {
    suspend operator fun invoke(claimId: String): AppResult<PendingCafeRegistrationClaimPreview> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                val preview = cafeRegistrationClaimRepository.approveCafeRegistrationClaim(claimId, currentUser.id)

                cafeRegistrationClaimEventPublisher.publish(
                    CafeRegistrationClaimEvent.Approved(
                        requesterUserId = preview.requesterUserId,
                        claimId = claimId
                    )
                )
                AppResult.Success(preview)
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
