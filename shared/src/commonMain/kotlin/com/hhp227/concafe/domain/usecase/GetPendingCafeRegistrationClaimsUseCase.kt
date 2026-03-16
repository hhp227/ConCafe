package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRegistrationClaimRepository

class GetPendingCafeRegistrationClaimsUseCase(
    private val authRepository: AuthRepository,
    private val cafeRegistrationClaimRepository: CafeRegistrationClaimRepository
) {
    suspend operator fun invoke(): AppResult<List<PendingCafeRegistrationClaimPreview>> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.ADMIN) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            AppResult.Success(cafeRegistrationClaimRepository.getPendingCafeRegistrationClaims())
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
