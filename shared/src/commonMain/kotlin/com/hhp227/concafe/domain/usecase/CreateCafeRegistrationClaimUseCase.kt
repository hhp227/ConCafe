package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.CafeRegistrationDraft
import com.hhp227.concafe.domain.model.PendingCafeRegistrationClaimPreview
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CafeRegistrationClaimRepository

class CreateCafeRegistrationClaimUseCase(
    private val authRepository: AuthRepository,
    private val cafeRegistrationClaimRepository: CafeRegistrationClaimRepository
) {
    suspend operator fun invoke(draft: CafeRegistrationDraft): AppResult<PendingCafeRegistrationClaimPreview> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            AppResult.Success(cafeRegistrationClaimRepository.createCafeRegistrationClaim(currentUser.id, draft))
        } catch (e: NoSuchElementException) {
            AppResult.Failure(AppError.NotFound)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
