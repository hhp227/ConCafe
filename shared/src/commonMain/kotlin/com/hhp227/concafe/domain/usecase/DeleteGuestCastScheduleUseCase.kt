package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository

class DeleteGuestCastScheduleUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(scheduleId: String): AppResult<Unit> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.CAFE_OWNER) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            castRepository.deleteGuestCastSchedule(scheduleId)
            AppResult.Success(Unit)
        } catch (e: IllegalArgumentException) {
            AppResult.Failure(AppError.ValidationFailed(e.message ?: "invalid request"))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
