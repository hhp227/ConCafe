package com.hhp227.concafe.domain.usecase

import com.hhp227.concafe.domain.common.AppError
import com.hhp227.concafe.domain.common.AppResult
import com.hhp227.concafe.domain.model.GuestCastSchedule
import com.hhp227.concafe.domain.model.UserRole
import com.hhp227.concafe.domain.repository.AuthRepository
import com.hhp227.concafe.domain.repository.CastRepository

class GetGuestCastSchedulesUseCase(
    private val authRepository: AuthRepository,
    private val castRepository: CastRepository
) {
    suspend operator fun invoke(cafeId: String, fromDate: String, toDate: String): AppResult<List<GuestCastSchedule>> {
        return try {
            val currentUser = authRepository.getCurrentUser()
                ?: return AppResult.Failure(AppError.Unauthorized)
            if (currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.CAFE_OWNER) {
                return AppResult.Failure(AppError.PermissionDenied)
            }
            if (cafeId.isBlank()) {
                return AppResult.Failure(AppError.ValidationFailed("cafeId is required"))
            }
            AppResult.Success(castRepository.getGuestCastSchedules(cafeId, fromDate, toDate))
        } catch (e: Exception) {
            AppResult.Failure(AppError.Unknown(e.message))
        }
    }
}
