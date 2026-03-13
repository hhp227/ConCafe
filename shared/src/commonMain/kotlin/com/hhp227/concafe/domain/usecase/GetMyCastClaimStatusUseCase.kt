package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.MyCastClaimStatus
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastClaimRepository

class GetMyCastClaimStatusUseCase(
    private val authRepository: AuthRepository,
    private val castClaimRepository: CastClaimRepository
) {
    suspend operator fun invoke(): AppResult<MyCastClaimStatus> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.CAST) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            AppResult.Success(castClaimRepository.getMyCastClaimStatus(currentUser.id))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
