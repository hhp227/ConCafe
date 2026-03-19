package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.event.CastClaimEvent
import com.hhp227.concafe.domain.event.publisher.CastClaimEventPublisher
import com.hhp227.concafe.domain.model.CastClaim
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastClaimRepository

class RejectCastClaimUseCase(
    private val authRepository: AuthRepository,
    private val castClaimRepository: CastClaimRepository,
    private val castClaimEventPublisher: CastClaimEventPublisher
) {
    suspend operator fun invoke(claimId: String): AppResult<CastClaim> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)

            if (currentUser.role != UserRole.CAFE_OWNER && currentUser.role != UserRole.ADMIN) {
                AppResult.Failure(AppError.PermissionDenied)
            } else {
                val claim = castClaimRepository.rejectCastClaim(claimId, currentUser.id)

                castClaimEventPublisher.publish(
                    CastClaimEvent.Updated(claim)
                )
                AppResult.Success(claim)
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
