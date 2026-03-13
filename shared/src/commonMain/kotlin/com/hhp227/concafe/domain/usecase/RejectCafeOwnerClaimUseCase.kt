package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.PendingCafeOwnerClaimPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeOwnerClaimRepository

class RejectCafeOwnerClaimUseCase(
    private val authRepository: AuthRepository,
    private val cafeOwnerClaimRepository: CafeOwnerClaimRepository
) {
    suspend operator fun invoke(claimId: String): AppResult<PendingCafeOwnerClaimPreview> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.ADMIN) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            AppResult.Success(cafeOwnerClaimRepository.rejectCafeOwnerClaim(claimId, currentUser.id))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
